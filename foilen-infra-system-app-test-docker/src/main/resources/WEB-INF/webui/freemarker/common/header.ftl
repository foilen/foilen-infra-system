<#import "/spring.ftl" as spring />
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">

  <title>Foilen Infra Plugin System - Fake</title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">

  <!-- Favicon -->
  <link rel="icon" type="image/png" href="/images/favicon.png" />
  <link rel="shortcut icon" type="image/png" href="/images/favicon.png" />

  <link href="<@spring.url'/bundles/all.css'/>" rel="stylesheet">
  <script src="<@spring.url'/bundles/all.js'/>"></script>
  
</head>
<body>

  <div class="container-fluid">

    <!-- Top menu -->
    <div class="navbar navbar-default" role="navigation">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
          <span class="sr-only">Toggle navigation</span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <a class="navbar-brand" href="/">Foilen Infra Plugin System - Fake</a>
      </div>
      <div class="navbar-collapse collapse">
        <ul class="nav navbar-nav">
          <li><a href="/plugin/list">Plugins</a></li>
          <li><a href="/resource/list">Resources</a></li>
        </ul>
        <ul class="nav navbar-nav navbar-right">
          <li><a href="?lang=<@spring.message "navbar.nextlang.id"/>"><@spring.message "navbar.nextlang.name"/></a></li>
        </ul>
      </div>
    </div>
    <!-- /Top menu -->

    <!-- Main -->
    <div class="row">

      <!-- Middle -->
      <div class="col-md-12">

      <#if errorCode??>
        <p class="bg-danger"><@spring.message errorCode errorParams/></p>
      </#if>
