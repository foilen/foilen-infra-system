<#include "/common/header.ftl">

<h1><@spring.message "resource.edit"/></h1>

<div class="fullMask"></div>
<form id="mainResource" data-resource-id="${resourceId}" data-resource-type="${resourceType}" data-editor-name="${editorName}" data-button-update='<@spring.message "button.edit"/>'><@spring.message "term.loading"/></form>

<#include "/common/footer.ftl">
