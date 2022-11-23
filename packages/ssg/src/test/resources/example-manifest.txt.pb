# proto-file: elide/meta/app.proto
# proto-message: AppManifest

app {
  endpoints {
    key: "801E583EA1CB9359D9BD36CC3378C6CB"
    value {
      name: "index:baseStyles"
      type: ASSET
      base: "/"
      tail: "/styles/base.css"
      produces: "text/css"
      method: GET
      options {
        precompilable: true
      }
      impl: "elide.docs.Index"
      member: "baseStyles"
      handler: "index"
    }
  }
  endpoints {
    key: "8172ABF0F999C3F61D871BADDF0E1177"
    value {
      name: "index:styles"
      type: ASSET
      base: "/"
      tail: "/styles/main.css"
      produces: "text/css"
      method: GET
      options {
        precompilable: true
        stateful: false
      }
      impl: "elide.docs.Index"
      member: "styles"
      handler: "index"
    }
  }
  endpoints {
    key: "AFCC882C0E5026CA0025A0CE910F8456"
    value {
      name: "index:indexPage"
      type: PAGE
      base: "/"
      tail: "/"
      produces: "text/html"
      method: GET
      options {
        precompilable: true
        stateful: false
      }
      impl: "elide.docs.Index"
      member: "indexPage"
      handler: "index"
    }
  }
  endpoints {
    key: "F75F25D6307CD50553D027F8C8DBFE58"
    value {
      name: "index:js"
      type: ASSET
      base: "/"
      tail: "/scripts/ui.js"
      produces: "application/javascript"
      method: GET
      options {
        precompilable: true
        stateful: false
      }
      impl: "elide.docs.Index"
      member: "js"
      handler: "index"
    }
  }
}
build {
  stamp {
    seconds: 1669169454
    nanos: 4481000
  }
}
