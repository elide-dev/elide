<#import "includes/page_metadata.ftl" as page_metadata>
<#import "includes/header.ftl" as header>
<#import "includes/footer.ftl" as footer>
<!DOCTYPE html>
<html class="no-js" prefix="og: https://ogp.me/ns#">
<head>
  <meta charset="utf-8" />
  <meta property="og:title" content="Elide Docs" />
  <meta property="og:type" content="website" />
  <meta property="og:url" content="https://docs.elide.dev/" />
  <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <@template_cmd name="pathToRoot"><script>var pathToRoot = "${pathToRoot}";</script></@template_cmd>
  <script>document.documentElement.classList.replace("no-js","js");</script>
    <#-- This script doesn't need to be there but it is nice to have
    since app in dark mode doesn't 'blink' (class is added before it is rendered) -->
  <script>const storage = localStorage.getItem("dokka-dark-mode")
    if (storage == null) {
      const osDarkSchemePreferred = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
      if (osDarkSchemePreferred === true) {
        document.getElementsByTagName("html")[0].classList.add("theme-dark")
      }
    } else {
      const savedDarkMode = JSON.parse(storage)
      if(savedDarkMode === true) {
        document.getElementsByTagName("html")[0].classList.add("theme-dark")
      }
    }
  </script>
  <#-- Resources (scripts, stylesheets) are handled by Dokka.
  Use customStyleSheets and customAssets to change them. -->
  <@resources/>
</head>
<body>
<div class="root">
    <@header.display/>
  <div id="container">
    <div class="sidebar" id="leftColumn">
      <div class="sidebar--inner" id="sideMenu"></div>
    </div>
    <div id="main">
        <@content/>
        <@footer.display/>
    </div>
  </div>
</div>
<script type="application/ld+json">
  {
    "@context": "https://schema.org",
    "@type": "SoftwareSourceCode",
    "name": "Elide",
    "alternateName": "Elide Framework",
    "runtimePlatform": "Java 17",
    "programmingLanguage": "Kotlin",
    "creativeWorkStatus": "Published",
    "codeRepository": "https://github.com/elide-dev/elide",
    "author": {
      "@type": "Person",
      "givenName": "Sam",
      "familyName": "Gammon",
      "url": "https://github.com/sgammon"
    },
    "publisher": {
      "@type": "Organization",
      "name": "Elide",
      "url": "https://elide.dev",
      "sameAs": [
        "https://github.com/elide-dev"
      ]
    }
  }
</script>
</body>
