const baseUrl = '/resource'

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

function updateAction(form, successfulUpdateHook) {
  // Mask + loading images
  jQuery('.fullMask').fadeIn();

  // Remove top error and fields errors
  jQuery('.topError', form).text('');
  jQuery('.help-inline', form).text('');

  // Get all the values
  var data = form.serializeArray();

  // Send them
  jQuery.ajax({
    type : "POST",
    url : baseUrl + '/update',
    data : data,
    success : function(result) {
      // Remove mask
      jQuery('.fullMask').fadeOut();

      // Check if there are values to update
      jQuery.each(result.fieldsValues, function(fieldName, valueText) {
        jQuery('#' + fieldName + ' input', form).val(valueText);
      });
      
      // Check if there are validation errors or a successful redirection url
      addTopErrorIfMissing(form);
      jQuery('.topError', form).text(result.topError);
      jQuery.each(result.fieldsErrors, function(fieldName, errorText) {
        fieldName = fieldName.replace('[' , '\\[')
        fieldName = fieldName.replace(']' , '\\]')
        jQuery('#' + fieldName + ' .help-inline', form).text(errorText);
      });

      // Check if there is an id
      var successResource = result.successResource;
      if (successResource) {
        successfulUpdateHook(successResource, result.successResourceId)
      }

    },
    error : function(jqXHR, textStatus) {
      // Remove mask and show error at the top
      jQuery('.fullMask').fadeOut();
      addTopErrorIfMissing(form);
      jQuery('.topError', form).text('Problem communicating with the site. Error: ' + textStatus);
    }
  });
}

function addTopErrorIfMissing(form) {
  if (jQuery('.topError', form).size() == 0) {
    form.prepend('<p class="topError text-danger"></p>');
  }
}

// Load the resource editor
function updateResourceEditor(form, config) {

  if (form.size() > 0) {
    addTopErrorIfMissing(form);
    var resourceId = form.data('resourceId');
    var resourceType = form.data('resourceType')
    if (resourceType) {
      resourceType = encodeURI(resourceType);
    }
    var editorName = form.data('editorName')
    if (editorName) {
      editorName = encodeURI(editorName);
    }
    
    var addButton = function() {
      
      // Editor field
      var editorSelect = jQuery('<select class="form-control editorName" />');
      form.prepend(editorSelect);

      jQuery.ajax({
        url : baseUrl + '/suggestEditor/' + resourceType,
        success : function(data) {
          // Set the options
          editorSelect.empty();
          data.forEach(function(option){
            editorSelect.append(jQuery('<option></option>').val(option).html(option))
          })
          
          // set the seleted one
          editorSelect.val(editorName)
          
          // When value change (selected)
          editorSelect.change(function() {
            form.data('editorName', editorSelect.val());
            updateResourceEditor(form, config);
          });
          
        }
      });

      // Update button
      var buttonUpdate = form.data('buttonUpdate');
      form.append('<hr /><div class="pull-right btn btn-success resourceUpdate">' + buttonUpdate + '</div>');
      jQuery('.resourceUpdate', form).click(function(){updateAction(form, config.successHook)});
      
      // Cancel button
      if (config.cancelButton) {
        var buttonCancel = jQuery('<div class="pull-right btn btn-primary cancel">' + config.cancelButton + '</div>');
        form.append(buttonCancel);
        buttonCancel.click(config.cancelHook);
      }
    }
    
    if (editorName) {
      // When there is a selected editor
      if (resourceId) { 
        // When it is an existing resource
        form.load(baseUrl + '/editPageDefinition/' + editorName + '/' + resourceId, function() {
          updateResourceFieldPageItems(form);
          addButton();
        });
      } else {
        // When it is a new resource
        form.load(baseUrl + '/createPageDefinition/' + editorName, function() {
          updateResourceFieldPageItems(form);
          addButton();
        });
      }
      
    } else {
      // When there is no selected editor
      form.load(baseUrl + '/createPageDefinitionByType/' + resourceType, function() {
        updateResourceFieldPageItems(form);
        addButton();
      });
    }
    
    form.submit(function(){
      return false;
    });
    
  }
}

jQuery(document).ready(function(){
  updateResourceEditor(jQuery('#mainResource'), {
    successHook: function(successResource, successResourceId) {
      window.location.href = baseUrl + '/edit/' + successResourceId;
    }
  });
});

//Create/Edit ListInputTextFieldPageItem
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

// Create/Edit resources (single one)
function updateResourceFieldPageItems(context) {
  jQuery('.ListInputTextFieldPageItem', context).each(function() {
    updateListInputTextFieldPageItem(jQuery(this));
  });
  jQuery('.ResourceFieldPageItem', context).each(function() {
    updateResourceFieldPageItem(jQuery(this));
  });
  jQuery('.ResourcesFieldPageItem', context).each(function() {
    updateResourcesFieldPageItem(jQuery(this));
  });
}

var dialogDepth = 0;

function updateResourceFieldPageItem(resource) {
  var buttonCreate = resource.data('buttonCreate');
  var buttonChoose = resource.data('buttonChoose');
  var buttonChange = resource.data('buttonChange');
  var buttonClose = resource.data('buttonClose');
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
    resource.append('<button class="btn btn-sm btn-success buttonCreate">' + buttonCreate + '</button>');
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

  // Create button
  jQuery('.buttonCreate', resource).click(function() {
    
    // Create dialog
    var dialog = jQuery('<form class="dialog"></form>');
    jQuery('body').append(dialog);
    ++dialogDepth;
    dialog.css({top: dialogDepth + '0px', left: dialogDepth + '0px'});
    
    dialog.data('buttonUpdate', buttonCreate);
    dialog.data('resourceType', resourceType);
    
    updateResourceEditor(dialog, {
      cancelButton: buttonClose,
      cancelHook: function() {
        dialog.remove();
        --dialogDepth;
      },
      successHook: function(successResource, successResourceId) {
        resource.data('resourceId', successResourceId);
        resource.data('resourceName', successResource.resourceName);
        resource.data('resourceDescription', successResource.resourceDescription);
        updateResourceFieldPageItem(resource);
        dialog.remove();
        --dialogDepth;
      },
    })
    
    return false;
  })
  
  // Choose and change button
  jQuery('.buttonChoose, .buttonChange', resource).click(function() {

    this.disabled = true;

    var typeahead = jQuery('.typeahead', resource);

    typeahead.fadeIn();

    var allData = [];

    jQuery.ajax({
      url : baseUrl + '/suggest/' + resourceType,
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
  var buttonCreate = resource.data('buttonCreate');
  var buttonClear = resource.data('buttonClear');
  var buttonClose = resource.data('buttonClose');
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
    itemsList.append('<li><button class="btn btn-sm btn-danger buttonRemove glyphicons glyphicons-remove" data-resource-id="' + item.resourceId + '"></button> ' + item.resourceName + ': ' + item.resourceDescription + '</li>');
  });
  resource.append('<button class="btn btn-sm btn-success buttonCreate">' + buttonCreate + '</button>');
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
    jQuery('.ResourcesFieldPageItem_resource[data-resource-id="' + resourceId + '"]', resource).remove();

    updateResourcesFieldPageItem(resource);
    return false;
  });

  // Create button
  jQuery('.buttonCreate', resource).click(function() {
    
    // Create dialog
    var dialog = jQuery('<form class="dialog"></form>');
    jQuery('body').append(dialog);
    ++dialogDepth;
    dialog.css({top: dialogDepth + '0px', left: dialogDepth + '0px'});
    
    dialog.data('buttonUpdate', buttonCreate);
    dialog.data('resourceType', resourceType);
    
    updateResourceEditor(dialog, {
      cancelButton: buttonClose,
      cancelHook: function() {
        dialog.remove();
        --dialogDepth;
      },
      successHook: function(successResource, successResourceId) {
        resource.append('<div class="ResourcesFieldPageItem_resource" data-resource-id="' + successResourceId //
            + '" data-resource-name="' + successResource.resourceName //
            + '" data-resource-description="' + successResource.resourceDescription + '"></div>');
        updateResourcesFieldPageItem(resource);
        dialog.remove();
        --dialogDepth;
      },
    })
    
    return false;
  })

  // Add button
  jQuery('.buttonAdd', resource).click(function() {

    this.disabled = true;

    var typeahead = jQuery('.typeahead', resource);

    typeahead.fadeIn();

    var allData = [];

    jQuery.ajax({
      url : baseUrl + '/suggest/' + resourceType,
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
