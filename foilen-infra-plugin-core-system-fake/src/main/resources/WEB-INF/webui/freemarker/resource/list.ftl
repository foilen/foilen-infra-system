<#include "/common/header.ftl">

<h1>Resources</h1>
<table class="table table-striped">
  <tr>
    <th>Type</th>
    <th>Name</th>
    <th>Description</th>
    <th>Actions</th>
  </tr>
  <#list resourcesByType?keys as type>
    <#list resourcesByType[type] as resource>
      <tr>
        <td>${type}</td>
        <td>${resource.resourceName}</td>
        <td>${resource.resourceDescription}</td>
        <td><a class="btn btn-primary" href="/resource/edit/${resource.internalId}"><@spring.message "button.edit"/></a></td>
      </tr>
   </#list>
  </#list>
</table>

<#include "/common/footer.ftl">
