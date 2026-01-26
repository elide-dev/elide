//! HTTP Client
//!
//! Implements HTTP/1.1 client for web requests,
//! with support for headers, methods, and chunked transfer.

use std::collections::BTreeMap;

/// HTTP method
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HttpMethod {
    Get,
    Post,
    Put,
    Delete,
    Head,
    Options,
    Patch,
}

impl HttpMethod {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Get => "GET",
            Self::Post => "POST",
            Self::Put => "PUT",
            Self::Delete => "DELETE",
            Self::Head => "HEAD",
            Self::Options => "OPTIONS",
            Self::Patch => "PATCH",
        }
    }
    
    pub fn from_str(s: &str) -> Option<Self> {
        match s.to_uppercase().as_str() {
            "GET" => Some(Self::Get),
            "POST" => Some(Self::Post),
            "PUT" => Some(Self::Put),
            "DELETE" => Some(Self::Delete),
            "HEAD" => Some(Self::Head),
            "OPTIONS" => Some(Self::Options),
            "PATCH" => Some(Self::Patch),
            _ => None,
        }
    }
}

/// HTTP version
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HttpVersion {
    Http10,
    Http11,
}

impl HttpVersion {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Http10 => "HTTP/1.0",
            Self::Http11 => "HTTP/1.1",
        }
    }
    
    pub fn from_str(s: &str) -> Option<Self> {
        match s {
            "HTTP/1.0" => Some(Self::Http10),
            "HTTP/1.1" => Some(Self::Http11),
            _ => None,
        }
    }
}

/// HTTP request
#[derive(Debug, Clone)]
pub struct HttpRequest {
    pub method: HttpMethod,
    pub path: String,
    pub version: HttpVersion,
    pub headers: BTreeMap<String, String>,
    pub body: Vec<u8>,
}

impl HttpRequest {
    pub fn new(method: HttpMethod, path: &str) -> Self {
        let mut headers = BTreeMap::new();
        headers.insert("Connection".to_string(), "close".to_string());
        
        Self {
            method,
            path: path.to_string(),
            version: HttpVersion::Http11,
            headers,
            body: Vec::new(),
        }
    }
    
    pub fn get(path: &str) -> Self {
        Self::new(HttpMethod::Get, path)
    }
    
    pub fn post(path: &str) -> Self {
        Self::new(HttpMethod::Post, path)
    }
    
    pub fn header(mut self, name: &str, value: &str) -> Self {
        self.headers.insert(name.to_string(), value.to_string());
        self
    }
    
    pub fn host(self, host: &str) -> Self {
        self.header("Host", host)
    }
    
    pub fn content_type(self, content_type: &str) -> Self {
        self.header("Content-Type", content_type)
    }
    
    pub fn user_agent(self, ua: &str) -> Self {
        self.header("User-Agent", ua)
    }
    
    pub fn body(mut self, body: Vec<u8>) -> Self {
        let len = body.len();
        self.body = body;
        self.headers.insert("Content-Length".to_string(), len.to_string());
        self
    }
    
    pub fn json(self, json: &str) -> Self {
        self.content_type("application/json")
            .body(json.as_bytes().to_vec())
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut request = Vec::new();
        
        // Request line
        request.extend_from_slice(self.method.as_str().as_bytes());
        request.push(b' ');
        request.extend_from_slice(self.path.as_bytes());
        request.push(b' ');
        request.extend_from_slice(self.version.as_str().as_bytes());
        request.extend_from_slice(b"\r\n");
        
        // Headers
        for (name, value) in &self.headers {
            request.extend_from_slice(name.as_bytes());
            request.extend_from_slice(b": ");
            request.extend_from_slice(value.as_bytes());
            request.extend_from_slice(b"\r\n");
        }
        
        // Empty line
        request.extend_from_slice(b"\r\n");
        
        // Body
        request.extend_from_slice(&self.body);
        
        request
    }
}

/// HTTP response
#[derive(Debug, Clone)]
pub struct HttpResponse {
    pub version: HttpVersion,
    pub status_code: u16,
    pub status_text: String,
    pub headers: BTreeMap<String, String>,
    pub body: Vec<u8>,
}

impl HttpResponse {
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        let text = core::str::from_utf8(data).ok()?;
        
        // Find end of headers
        let header_end = text.find("\r\n\r\n")?;
        let header_section = &text[..header_end];
        
        let mut lines = header_section.lines();
        
        // Status line
        let status_line = lines.next()?;
        let mut parts = status_line.split_whitespace();
        
        let version = HttpVersion::from_str(parts.next()?)?;
        let status_code: u16 = parts.next()?.parse().ok()?;
        let status_text: String = parts.collect::<Vec<_>>().join(" ");
        
        // Headers
        let mut headers = BTreeMap::new();
        for line in lines {
            if let Some(colon_pos) = line.find(':') {
                let name = line[..colon_pos].trim().to_string();
                let value = line[colon_pos + 1..].trim().to_string();
                headers.insert(name.to_lowercase(), value);
            }
        }
        
        // Determine body length
        let body_start = header_end + 4;
        let body_len = if let Some(len_str) = headers.get("content-length") {
            len_str.parse::<usize>().ok()?
        } else if headers.get("transfer-encoding").map(|s| s.contains("chunked")).unwrap_or(false) {
            // Chunked transfer - parse chunks
            return Self::parse_chunked(data, body_start, version, status_code, status_text, headers);
        } else {
            // Read until connection close
            data.len() - body_start
        };
        
        if data.len() < body_start + body_len {
            return None;  // Incomplete
        }
        
        let body = data[body_start..body_start + body_len].to_vec();
        
        Some((Self {
            version,
            status_code,
            status_text,
            headers,
            body,
        }, body_start + body_len))
    }
    
    fn parse_chunked(
        data: &[u8],
        body_start: usize,
        version: HttpVersion,
        status_code: u16,
        status_text: String,
        headers: BTreeMap<String, String>,
    ) -> Option<(Self, usize)> {
        let mut body = Vec::new();
        let mut offset = body_start;
        
        loop {
            // Find chunk size line
            let chunk_header_end = find_crlf(&data[offset..])?;
            let chunk_header = core::str::from_utf8(&data[offset..offset + chunk_header_end]).ok()?;
            let chunk_size = usize::from_str_radix(chunk_header.trim(), 16).ok()?;
            
            offset += chunk_header_end + 2;  // Skip CRLF
            
            if chunk_size == 0 {
                offset += 2;  // Final CRLF
                break;
            }
            
            if data.len() < offset + chunk_size + 2 {
                return None;  // Incomplete
            }
            
            body.extend_from_slice(&data[offset..offset + chunk_size]);
            offset += chunk_size + 2;  // Skip chunk data and CRLF
        }
        
        Some((Self {
            version,
            status_code,
            status_text,
            headers,
            body,
        }, offset))
    }
    
    pub fn is_success(&self) -> bool {
        self.status_code >= 200 && self.status_code < 300
    }
    
    pub fn is_redirect(&self) -> bool {
        self.status_code >= 300 && self.status_code < 400
    }
    
    pub fn is_error(&self) -> bool {
        self.status_code >= 400
    }
    
    pub fn header(&self, name: &str) -> Option<&str> {
        self.headers.get(&name.to_lowercase()).map(|s| s.as_str())
    }
    
    pub fn content_type(&self) -> Option<&str> {
        self.header("content-type")
    }
    
    pub fn content_length(&self) -> Option<usize> {
        self.header("content-length")?.parse().ok()
    }
    
    pub fn location(&self) -> Option<&str> {
        self.header("location")
    }
    
    pub fn body_text(&self) -> Option<String> {
        String::from_utf8(self.body.clone()).ok()
    }
}

fn find_crlf(data: &[u8]) -> Option<usize> {
    for i in 0..data.len().saturating_sub(1) {
        if data[i] == b'\r' && data[i + 1] == b'\n' {
            return Some(i);
        }
    }
    None
}

/// URL parser
#[derive(Debug, Clone)]
pub struct Url {
    pub scheme: String,
    pub host: String,
    pub port: u16,
    pub path: String,
    pub query: Option<String>,
}

impl Url {
    pub fn parse(url: &str) -> Option<Self> {
        let (scheme, rest) = if url.starts_with("https://") {
            ("https".to_string(), &url[8..])
        } else if url.starts_with("http://") {
            ("http".to_string(), &url[7..])
        } else {
            return None;
        };
        
        let default_port = if scheme == "https" { 443 } else { 80 };
        
        // Split host from path
        let (host_port, path_query) = if let Some(slash_pos) = rest.find('/') {
            (&rest[..slash_pos], &rest[slash_pos..])
        } else {
            (rest, "/")
        };
        
        // Parse host and port
        let (host, port) = if let Some(colon_pos) = host_port.rfind(':') {
            let port_str = &host_port[colon_pos + 1..];
            if let Ok(port) = port_str.parse::<u16>() {
                (host_port[..colon_pos].to_string(), port)
            } else {
                (host_port.to_string(), default_port)
            }
        } else {
            (host_port.to_string(), default_port)
        };
        
        // Parse path and query
        let (path, query) = if let Some(q_pos) = path_query.find('?') {
            (path_query[..q_pos].to_string(), Some(path_query[q_pos + 1..].to_string()))
        } else {
            (path_query.to_string(), None)
        };
        
        Some(Self { scheme, host, port, path, query })
    }
    
    pub fn is_https(&self) -> bool {
        self.scheme == "https"
    }
    
    pub fn request_path(&self) -> String {
        if let Some(query) = &self.query {
            format!("{}?{}", self.path, query)
        } else {
            self.path.clone()
        }
    }
    
    pub fn host_header(&self) -> String {
        if (self.scheme == "http" && self.port == 80) ||
           (self.scheme == "https" && self.port == 443) {
            self.host.clone()
        } else {
            format!("{}:{}", self.host, self.port)
        }
    }
}

/// HTTP client state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum HttpClientState {
    #[default]
    Idle,
    Connecting,
    TlsHandshaking,
    SendingRequest,
    ReceivingResponse,
    Complete,
    Error,
}

/// HTTP client
pub struct HttpClient {
    state: HttpClientState,
    url: Option<Url>,
    request: Option<HttpRequest>,
    response_buffer: Vec<u8>,
    max_redirects: u8,
    redirects: u8,
    timeout_ms: u64,
    user_agent: String,
}

impl HttpClient {
    pub fn new() -> Self {
        Self {
            state: HttpClientState::Idle,
            url: None,
            request: None,
            response_buffer: Vec::new(),
            max_redirects: 5,
            redirects: 0,
            timeout_ms: 30000,
            user_agent: "Colide/1.0".to_string(),
        }
    }
    
    pub fn timeout(mut self, ms: u64) -> Self {
        self.timeout_ms = ms;
        self
    }
    
    pub fn user_agent(mut self, ua: &str) -> Self {
        self.user_agent = ua.to_string();
        self
    }
    
    pub fn max_redirects(mut self, max: u8) -> Self {
        self.max_redirects = max;
        self
    }
    
    /// Start GET request
    pub fn get(&mut self, url: &str) -> Result<HttpAction, HttpError> {
        self.request_with_method(HttpMethod::Get, url, None)
    }
    
    /// Start POST request
    pub fn post(&mut self, url: &str, body: Option<Vec<u8>>) -> Result<HttpAction, HttpError> {
        self.request_with_method(HttpMethod::Post, url, body)
    }
    
    fn request_with_method(&mut self, method: HttpMethod, url: &str, body: Option<Vec<u8>>) -> Result<HttpAction, HttpError> {
        let parsed_url = Url::parse(url).ok_or(HttpError::InvalidUrl)?;
        
        let mut request = HttpRequest::new(method, &parsed_url.request_path())
            .host(&parsed_url.host_header())
            .user_agent(&self.user_agent);
        
        if let Some(body_data) = body {
            request = request.body(body_data);
        }
        
        let is_https = parsed_url.is_https();
        let host = parsed_url.host.clone();
        let port = parsed_url.port;
        
        self.url = Some(parsed_url);
        self.request = Some(request);
        self.response_buffer.clear();
        self.state = HttpClientState::Connecting;
        
        Ok(HttpAction::Connect { host, port, tls: is_https })
    }
    
    /// Called when TCP connection is established
    pub fn on_connected(&mut self) -> HttpAction {
        if let Some(url) = &self.url {
            if url.is_https() {
                self.state = HttpClientState::TlsHandshaking;
                HttpAction::StartTls { hostname: url.host.clone() }
            } else {
                self.send_request()
            }
        } else {
            HttpAction::None
        }
    }
    
    /// Called when TLS handshake completes
    pub fn on_tls_ready(&mut self) -> HttpAction {
        self.send_request()
    }
    
    fn send_request(&mut self) -> HttpAction {
        if let Some(request) = &self.request {
            self.state = HttpClientState::SendingRequest;
            HttpAction::Send(request.build())
        } else {
            HttpAction::None
        }
    }
    
    /// Called when request is sent
    pub fn on_sent(&mut self) -> HttpAction {
        self.state = HttpClientState::ReceivingResponse;
        HttpAction::Receive
    }
    
    /// Process received data
    pub fn on_data(&mut self, data: &[u8]) -> HttpAction {
        self.response_buffer.extend_from_slice(data);
        
        // Try to parse response
        if let Some((response, _)) = HttpResponse::parse(&self.response_buffer) {
            // Handle redirects
            if response.is_redirect() && self.redirects < self.max_redirects {
                if let Some(location) = response.location() {
                    self.redirects += 1;
                    
                    // Handle relative URLs
                    let redirect_url = if location.starts_with("http") {
                        location.to_string()
                    } else if let Some(url) = &self.url {
                        format!("{}://{}{}", url.scheme, url.host_header(), location)
                    } else {
                        return HttpAction::Error(HttpError::InvalidRedirect);
                    };
                    
                    // Start new request
                    if let Ok(action) = self.get(&redirect_url) {
                        return action;
                    }
                }
            }
            
            self.state = HttpClientState::Complete;
            return HttpAction::Response(response);
        }
        
        // Need more data
        HttpAction::Receive
    }
    
    /// Handle error
    pub fn on_error(&mut self, error: HttpError) -> HttpAction {
        self.state = HttpClientState::Error;
        HttpAction::Error(error)
    }
    
    pub fn state(&self) -> HttpClientState {
        self.state
    }
    
    pub fn reset(&mut self) {
        self.state = HttpClientState::Idle;
        self.url = None;
        self.request = None;
        self.response_buffer.clear();
        self.redirects = 0;
    }
}

impl Default for HttpClient {
    fn default() -> Self {
        Self::new()
    }
}

/// HTTP action
#[derive(Debug)]
pub enum HttpAction {
    None,
    Connect { host: String, port: u16, tls: bool },
    StartTls { hostname: String },
    Send(Vec<u8>),
    Receive,
    Response(HttpResponse),
    Error(HttpError),
}

/// HTTP error
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HttpError {
    InvalidUrl,
    ConnectionFailed,
    TlsFailed,
    Timeout,
    InvalidResponse,
    TooManyRedirects,
    InvalidRedirect,
}

/// Simple HTTP header builder for common patterns
pub struct HeaderBuilder {
    headers: BTreeMap<String, String>,
}

impl HeaderBuilder {
    pub fn new() -> Self {
        Self { headers: BTreeMap::new() }
    }
    
    pub fn add(mut self, name: &str, value: &str) -> Self {
        self.headers.insert(name.to_string(), value.to_string());
        self
    }
    
    pub fn accept(self, content_type: &str) -> Self {
        self.add("Accept", content_type)
    }
    
    pub fn accept_json(self) -> Self {
        self.accept("application/json")
    }
    
    pub fn authorization(self, auth: &str) -> Self {
        self.add("Authorization", auth)
    }
    
    pub fn bearer_token(self, token: &str) -> Self {
        self.authorization(&format!("Bearer {}", token))
    }
    
    pub fn content_type(self, content_type: &str) -> Self {
        self.add("Content-Type", content_type)
    }
    
    pub fn build(self) -> BTreeMap<String, String> {
        self.headers
    }
}

impl Default for HeaderBuilder {
    fn default() -> Self {
        Self::new()
    }
}

/// Query string builder
pub struct QueryBuilder {
    params: Vec<(String, String)>,
}

impl QueryBuilder {
    pub fn new() -> Self {
        Self { params: Vec::new() }
    }
    
    pub fn add(mut self, key: &str, value: &str) -> Self {
        self.params.push((key.to_string(), value.to_string()));
        self
    }
    
    pub fn build(&self) -> String {
        if self.params.is_empty() {
            return String::new();
        }
        
        self.params.iter()
            .map(|(k, v)| format!("{}={}", url_encode(k), url_encode(v)))
            .collect::<Vec<_>>()
            .join("&")
    }
}

impl Default for QueryBuilder {
    fn default() -> Self {
        Self::new()
    }
}

fn url_encode(s: &str) -> String {
    let mut result = String::new();
    for c in s.chars() {
        match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '-' | '_' | '.' | '~' => {
                result.push(c);
            }
            ' ' => result.push('+'),
            _ => {
                for b in c.to_string().as_bytes() {
                    result.push_str(&format!("%{:02X}", b));
                }
            }
        }
    }
    result
}
