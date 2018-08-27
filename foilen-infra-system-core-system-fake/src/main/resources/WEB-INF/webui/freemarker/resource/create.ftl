<#include "/common/header.ftl">

<h1><@spring.message "resource.create"/></h1>

<p id="topError" class="text-danger"></p>
<div class="fullMask"></div>
<form id="mainResource" autocomplete="off" data-editor-name="${editorName}" data-resource-type="${resourceType}" data-button-update='<@spring.message "button.create"/>'><@spring.message "term.loading"/></form>

<#include "/common/footer.ftl">