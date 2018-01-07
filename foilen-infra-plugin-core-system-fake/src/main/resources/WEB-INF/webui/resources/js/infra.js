// Form submission confirmation
jQuery(document).ready(function() {
  jQuery('form.confirm').submit(function() {
    var form = jQuery(this);
    return confirm(form.data('confirm'));
  });
});

// Press enter to execute action. (enterControlled and enterAction)
jQuery(document).ready(function() {
  jQuery('.enterControlled').keypress(function(e) {
    if (e.which == 13) {
      var parent = jQuery(this).parent();
      jQuery('.enterAction', parent).click();
      return false;
    }

    return true;
  });
});

// Load the resource editor
function updateResourceEditor() {
  var mainResource = jQuery('#mainResource');
  if (mainResource.size() > 0) {
    var resourceId = mainResource.data('resourceId');
    var editorName = encodeURI(mainResource.data('editorName'));
    if (editorName) {
      if (resourceId) {
        mainResource.load('/resource/editPageDefinition/' + editorName + '/' + resourceId, function() {
          updateResourceFieldPageItems();
          updateListInputTextFieldPageItems();
        });

        jQuery('#editorName').change(function() {
          mainResource.data('editorName', jQuery(this).val());

          // Remove all events
          jQuery('#editorName').unbind();
          jQuery('#mainResource').unbind();
          jQuery('button.resourceUpdate').unbind();

          // Update
          updateResourceEditor();
        });
      } else {
        mainResource.load('/resource/createPageDefinition/' + editorName, function() {
          updateResourceFieldPageItems();
          updateListInputTextFieldPageItems();
        });
      }
      
      var updateFunction = function() {
        // Mask + loading images
        jQuery('.fullMask').fadeIn();

        // Remove top error and fields errors
        jQuery('#topError').text('');
        jQuery('.help-inline').text('');

        // Get all the values
        var data = jQuery('#mainResource').serializeArray();

        // Send them
        jQuery.ajax({
          type : "POST",
          url : '/resource/update',
          data : data,
          success : function(result) {
            // Remove mask
            jQuery('.fullMask').fadeOut();

            // Check if there are values to update
            jQuery.each(result.fieldsValues, function(fieldName, valueText) {
              jQuery('#' + fieldName + ' input').val(valueText);
            });

            // Check if there are validation errors or a successful redirection url
            jQuery('#topError').text(result.topError);
            jQuery.each(result.fieldsErrors, function(fieldName, errorText) {
              fieldName = fieldName.replace('[' , '\\[')
              fieldName = fieldName.replace(']' , '\\]')
              jQuery('#' + fieldName + ' .help-inline').text(errorText);
            });

            // Check if there is an id
            var successResourceId = result.successResourceId;
            if (successResourceId) {
              window.location.href = '/resource/edit/' + successResourceId;
            }

          },
          error : function(jqXHR, textStatus) {
            // Remove mask and show error at the top
            jQuery('.fullMask').fadeOut();
            jQuery('#topError').text('Problem communicating with the site. Error: ' + textStatus);
          }
        });
        
        return false;
      }

      jQuery('button.resourceUpdate').click(updateFunction);
      mainResource.submit(updateFunction);

    } else {
      mainResource.text('BAD REQUEST');
    }
  }
}

jQuery(document).ready(updateResourceEditor);



//Create/Edit ListInputTextFieldPageItem
function updateListInputTextFieldPageItems() {
  jQuery('.ListInputTextFieldPageItem').each(function() {
    updateListInputTextFieldPageItem(jQuery(this));
  });
}

function updateListInputTextFieldPageItem(listBlock) {
  var fieldName = listBlock.data('fieldName');
  
  jQuery('.buttonDelete', listBlock).click(function() {
    jQuery(this).parent().remove();
    return false;
  });
  
  
  // Controls
  jQuery('.buttonAdd', listBlock).click(function() {
    var newItem = jQuery('.template', listBlock).clone();
    var newItemId = fieldName + '[' + new Date().getTime() + ']';
    newItem.attr('id', newItemId);
    newItem.removeClass('template');
    jQuery('input', newItem).attr('name', newItemId);
    newItem.insertBefore(jQuery('.controls', listBlock));
    
    // Hook delete
    jQuery('.buttonDelete', newItem).click(function() {
      jQuery(this).parent().remove();
      return false;
    });
    
    return false;
  });
}

jQuery(document).ready(updateListInputTextFieldPageItems);

// Create/Edit resources (single one)
function updateResourceFieldPageItems() {
  jQuery('.ResourceFieldPageItem').each(function() {
    updateResourceFieldPageItem(jQuery(this));
  });
  jQuery('.ResourcesFieldPageItem').each(function() {
    updateResourcesFieldPageItem(jQuery(this));
  });
}

function updateResourceFieldPageItem(resource) {
  var buttonChoose = resource.data('buttonChoose');
  var buttonChange = resource.data('buttonChange');
  var buttonClear = resource.data('buttonClear');
  var label = resource.data('label');
  var fieldName = resource.data('fieldName');
  var resourceType = resource.data('resourceType');
  var resourceId = resource.data('resourceId');
  var resourceName = resource.data('resourceName');
  var resourceDescription = resource.data('resourceDescription');

  resource.empty();

  // Common
  resource.append('<label for="' + fieldName + '">' + label + '</label>');

  if (resourceId) {
    // Present
    resource.append('<input type="hidden" name="' + fieldName + '" value="' + resourceId + '"/>');
    resource.append('<p>' + resourceName + ': ' + resourceDescription + '</p>');
    resource.append('<button class="btn btn-sm btn-info buttonChange">' + buttonChange + '</button>');
    resource.append('<button class="btn btn-sm btn-danger buttonClear">' + buttonClear + '</button>');
  } else {
    // Not there
    resource.append('<br/>');
    resource.append('<button class="btn btn-sm btn-info buttonChoose">' + buttonChoose + '</button>');
  }

  // Common
  resource.append('<br/><input style="display: none;" class="form-control typeahead" type="text"/>');
  resource.append('<span class="help-inline text-danger"></span>');

  // Clear button
  jQuery('.buttonClear', resource).click(function() {
    resource.data('resourceId', '');
    resource.data('resourceName', '');
    resource.data('resourceDescription', '');
    updateResourceFieldPageItem(resource);
    return false;
  });

  // Choose and change button
  jQuery('.buttonChoose, .buttonChange', resource).click(function() {

    this.disabled = true;

    var typeahead = jQuery('.typeahead', resource);

    typeahead.fadeIn();

    var allData = [];

    jQuery.ajax({
      url : '/resource/suggest/' + resourceType,
      success : function(data) {
        allData = data;
      }
    });

    typeahead.typeahead({
      highlight : true,
      minLength : 0,
      hint : true,
    }, {
      display : function(data) {
        return data.name + ': ' + data.description;
      },
      limit : 25,
      source : function(query, syncResults) {
        var searchedData = [];
        query = query.toLowerCase();
        jQuery.each(allData, function() {
          if (this.name.toLowerCase().indexOf(query) !== -1 || this.description.toLowerCase().indexOf(query) !== -1) {
            searchedData.push(this);
          }
        });
        syncResults(searchedData);
      },
    });

    // When value change (selected)
    typeahead.bind('typeahead:select', function(event, selected) {
      resource.data('resourceId', selected.id);
      resource.data('resourceName', selected.name);
      resource.data('resourceDescription', selected.description);
      updateResourceFieldPageItem(resource);
    });

    return false;
  });

}

// Create/Edit resources (multiple)
function updateResourcesFieldPageItem(resource) {
  var buttonAdd = resource.data('buttonAdd');
  var buttonClear = resource.data('buttonClear');
  var label = resource.data('label');
  var fieldName = resource.data('fieldName');
  var resourceType = resource.data('resourceType');

  var items = []
  jQuery('.ResourcesFieldPageItem_resource', resource).each(function() {
    var item = jQuery(this);
    items.push({
      resourceId : item.data('resourceId'),
      resourceName : item.data('resourceName'),
      resourceDescription : item.data('resourceDescription'),
    });
  });

  resource.empty();

  // Add the items back
  var itemsIds = [];
  var itemsIdsComma = "";
  jQuery.each(items, function(_, item) {
    if (itemsIdsComma == "") {
      itemsIdsComma += item.resourceId;
    } else {
      itemsIdsComma += ',' + item.resourceId;
    }
    itemsIds.push(item.resourceId);
    resource.append('<div class="ResourcesFieldPageItem_resource" data-resource-id="' + item.resourceId //
        + '" data-resource-name="' + item.resourceName //
        + '" data-resource-description="' + item.resourceDescription + '"></div>');
  });

  // The label
  resource.append('<label for="' + fieldName + '">' + label + '</label>');

  resource.append('<input type="hidden" name="' + fieldName + '" value="' + itemsIds + '"/>');

  resource.append('<ul></ul>');
  var itemsList = jQuery('ul', resource);
  jQuery.each(items, function(_, item) {
    itemsList.append('<li><button class="btn btn-sm btn-danger buttonRemove glyphicons glyphicons-remove" data-resource-id="'
        + item.resourceId + '"></button> ' + item.resourceName + ': ' + item.resourceDescription + '</li>');
  });
  resource.append('<button class="btn btn-sm btn-info buttonAdd">' + buttonAdd + '</button>');
  resource.append('<button class="btn btn-sm btn-danger buttonClear">' + buttonClear + '</button>');

  resource.append('<br/><input style="display:none" class="form-control typeahead" type="text"/>');
  resource.append('<span class="help-inline text-danger"></span>');

  // Clear button
  jQuery('.buttonClear', resource).click(function() {
    resource.empty();
    updateResourcesFieldPageItem(resource);
    return false;
  });

  // Remove button
  jQuery('.buttonRemove', resource).click(function() {

    var resourceId = jQuery(this).data('resourceId');
    console.log(resourceId)
    jQuery('.ResourcesFieldPageItem_resource[data-resource-id="' + resourceId + '"]', resource).remove();

    updateResourcesFieldPageItem(resource);
    return false;
  });

  // Add button
  jQuery('.buttonAdd', resource).click(function() {

    this.disabled = true;

    var typeahead = jQuery('.typeahead', resource);

    typeahead.fadeIn();

    var allData = [];

    jQuery.ajax({
      url : '/resource/suggest/' + resourceType,
      success : function(data) {
        allData = data;
      }
    });

    typeahead.typeahead({
      highlight : true,
      minLength : 0,
      hint : true,
    }, {
      display : function(data) {
        return data.name + ': ' + data.description;
      },
      limit : 25,
      source : function(query, syncResults) {
        var searchedData = [];
        query = query.toLowerCase();
        jQuery.each(allData, function() {
          if (jQuery.inArray(this.id, itemsIds) > -1) {
            return;
          }
          if (this.name.toLowerCase().indexOf(query) !== -1 || this.description.toLowerCase().indexOf(query) !== -1) {
            searchedData.push(this);
          }
        });
        syncResults(searchedData);
      },
    });

    // When value change (selected)
    typeahead.bind('typeahead:select', function(event, selected) {
      resource.append('<div class="ResourcesFieldPageItem_resource" data-resource-id="' + selected.id //
          + '" data-resource-name="' + selected.name //
          + '" data-resource-description="' + selected.description + '"></div>');
      updateResourcesFieldPageItem(resource);
    });

    return false;
  });

}
