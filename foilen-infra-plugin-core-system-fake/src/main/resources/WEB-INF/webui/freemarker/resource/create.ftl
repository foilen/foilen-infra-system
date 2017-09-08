<#include "/common/header.ftl">

<h1><@spring.message "resource.create"/></h1>

<p id="topError" class="text-danger"></p>
<div class="fullMask"></div>
<form id="mainResource" data-editor-name="${editorName}"><@spring.message "term.loading"/></form>

<hr/>
<button class="pull-right btn btn-success resourceUpdate"><@spring.message "button.create"/></button>

<#include "/common/footer.ftl">
