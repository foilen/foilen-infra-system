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
        <td>
          
          <#assign resourceNameArgs = [resource.resourceName]/>
          <form class="confirm form-inline" method="post" action="/resource/delete" data-confirm="<@spring.messageArgs 'prompt.delete.confirm' resourceNameArgs />">

            <input type="hidden" name="resourceId" value="${resource.internalId}" />

            <a class="btn btn-sm btn-primary" href="/resource/edit/${resource.internalId}"><@spring.message "button.edit"/></a>
        
            <button class="btn btn-sm btn-danger"><@spring.message 'button.delete'/></button>  
  
          </form>

        </td>
      </tr>
   </#list>
  </#list>
</table>

<#include "/common/footer.ftl">
