<#include "/common/header.ftl">

<h1><@spring.message "resource.edit"/></h1>

<p id="topError" class="text-danger"></p>
<div class="fullMask"></div>
<div class="pull-right">
  <select id="editorName">
    <#list editorNames as item>
      <option value="${item}" ${(editorName == item)?then('selected="selected"','')}>${item}</option>
    </#list>
  </select>
</div>
<form id="mainResource" data-resource-id="${resourceId}" data-editor-name="${editorName}"><@spring.message "term.loading"/></form>

<hr/>
<button class="pull-right btn btn-success resourceUpdate"><@spring.message "button.edit"/></button>

<#include "/common/footer.ftl">
