<#include "/common/header.ftl">

<h1>Resources</h1>
<div>
  <form class="form-horizontal" action="/resource/exportFolder" method="post">
    <div class="form-group">
      <label class="col-xs-3" for="folder"><@spring.message "export.folder" /></label> 
      <div class="col-xs-9">
        <input class="form-control" id="folder" name="folder" type="text" />
      </div>
    </div>
  </form>
</div>
<div>
  <form action="/resource/exportFile" method="get">
    <button class="btn btn-sm btn-info"><@spring.message "export.file" /></button> 
  </form>
</div>
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
