<#include "/common/header.ftl">

<h1>Available</h1>
<table class="table table-striped">
  <tr>
    <th>Name</th>
    <th>Vendor</th>
    <th>Description</th>
    <th>Version</th>
    <th>Custom Resources</th>
    <th>Resources Editors</th>
    <th>Timers</th>
  </tr>
  <#list availables as item>
    <tr>
      <td>${item.pluginName}</td>
      <td>${item.pluginVendor}</td>
      <td>${item.pluginDescription}</td>
      <td>${item.pluginVersion}</td>
      <td>
        <ul>
          <#list item.customResources as customResource>
            <li><strong>${customResource.resourceType}</strong> (${customResource.resourceClass.name})</li>
          </#list>            
        </ul>
      </td>
      <td>
        <ul>
          <#list item.resourceEditors as resourceEditor>
            <li><a href="/resource/create/${resourceEditor.editorName}">${resourceEditor.editorName}</a> for ${resourceEditor.editor.forResourceType.name}</li>
          </#list>            
        </ul>
      </td>
      <td>
        <ul>
          <#list item.timers as timer>
            <li>${timer.timerName} | ${timer.deltaTime} ${timer.calendarUnitInText}</li>
          </#list>            
        </ul>
      </td>
    </tr>
  </#list>
</table>

<h1>Broken</h1>
<table class="table table-striped">
  <tr>
    <th>Class</th>
    <th>Vendor</th>
    <th>Name</th>
    <th>Description</th>
    <th>Version</th>
    <th>Error</th>
  </tr>
  <#list brokens as item>
    <tr>
      <td>${item.a.name}</td>
      <#if item.b??>
        <td>${item.b.pluginVendor}</td>
        <td>${item.b.pluginName}</td>
        <td>${item.b.pluginDescription}</td>
        <td>${item.b.pluginVersion}</td>
      <#else>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
      </#if>
      <td>${item.c}</td>
    </tr>
  </#list>
</table>


<#include "/common/footer.ftl">