<#include "/common/header.ftl">

<h1><@spring.message "resource.edit"/></h1>

<p id="topError" class="text-danger"></p>
<div class="fullMask"></div>
<form id="mainResource" autocomplete="off" data-resource-id="${resourceId}" data-resource-type="${resourceType}" data-editor-name="${editorName}" data-button-update='<@spring.message "button.edit"/>'><@spring.message "term.loading"/></form>

<#include "/common/footer.ftl">
