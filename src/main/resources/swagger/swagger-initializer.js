window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  window.ui = SwaggerUIBundle({
    url: "/api-docs/swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });

  window.ui.initOAuth({
    clientId: "swagger-ui",
    clientSecret: "ZXhhbXBsZS1hcHAtc2VjcmV0",
    realm: "service",
    appName: "swagger-ui",
    scopeSeparator: " ",
    scopes: "openid profile",
    additionalQueryStringParams: {"nonce": "132456"},
    usePkceWithAuthorizationCodeGrant: true
  })
  //</editor-fold>
};
