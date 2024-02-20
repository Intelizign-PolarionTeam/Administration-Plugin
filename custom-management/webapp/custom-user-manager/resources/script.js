$(document).ready(function() {
	$.ajax({
		url: 'usermana?action=getProjectList',
		type: 'GET',
		dataType: 'json',
		success: function(response) {
			const projectsList = response.projectsList;
			const prePostHookMapObj = response.prePostHookMapObj
			$('#pre-post a').on('click', function() {
				showPopupForPrePost('Pre-Post Save Details', prePostHookMapObj);
			});
			if (response) {
				var projectSelect = $('.form-control');
				projectSelect.empty();
				projectSelect.append($('<option>', { value: '', text: 'Select a project' }));
				$.each(projectsList, function(id, name) {
					projectSelect.append($('<option>', {
						value: id,
						text: name
					}));
				});
			} else {
				console.error('Failed to retrieve project list');
			}
		},
		error: function(error) {
			console.error('Error occurred while fetching project list:', error);
		}
	});
});

function projectInfo() {
	const projectId = $("#projectDropDown").val();
	alert("Selected project Id is: " + projectId);
	$('.polarion-rpw-table-content').show();
	$('#pre-post').css('display', 'block');
	$.ajax({
		url: `usermana?action=getCustomizationCountDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",
		success: function(data) {

			const wiCustomizationObj = data.customizationCountDetails;
			const moduleCustomizationObj = data.moduleCustomizationDetails;
			console.log("customizationDetails:", wiCustomizationObj);
			console.log("moduleCustomizationObj:", moduleCustomizationObj);
			// Clear existing table content
			$('#userTableBody').empty();
			$('#userTableBodyDocument').empty();

			workItemCustomizationTable(wiCustomizationObj);
			moduleCustomizationTable(moduleCustomizationObj);
		},
		error: function(error) {
			console.error("Error occurred:", error);
		}
	});
}

function workItemCustomizationTable(wiCustomizationObj) {
	$.each(wiCustomizationObj, function(index, wiCustom) {
		console.log("wiCustom object:", wiCustom);
		if (wiCustom.hasOwnProperty('wiType') && wiCustom.hasOwnProperty('wiName') &&
			wiCustom.hasOwnProperty('customFieldCount') && wiCustom.hasOwnProperty('scriptCount') &&
			wiCustom.hasOwnProperty('customEnumerationCount') && wiCustom.hasOwnProperty('scriptFunctionCount')) {

			console.log("wiName is", wiCustom.wiName);
			var row = $('<tr>').addClass('table-content-row');
			row.append($('<td>').text(wiCustom.wiName).css('text-align', 'center'));

			['customFieldCount', 'scriptCount', 'customEnumerationCount', 'scriptFunctionCount'].forEach(function(countType) {
				var count = wiCustom[countType];
				var countCell = $('<td>').css('text-align', 'center');
				if (count > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell')
						.text(count)
						.data('heading', wiCustom.wiType)
						.data('type', countType);
						
					countCell.append(hyperlink);
				} else {
					countCell.text(count);
				}
				row.append(countCell);
			});

			$('#userTableBody').append(row);
		}
	});
}

function moduleCustomizationTable(moduleCustomizationObj) {
	$.each(moduleCustomizationObj, function(index, moduleCustom) {
		console.log("moduleCustom object:", moduleCustom);

		if (moduleCustom.hasOwnProperty('moduleType') && moduleCustom.hasOwnProperty('moduleCustomFieldCount') &&
			moduleCustom.hasOwnProperty('moduleWorkflowFunctionCount') && moduleCustom.hasOwnProperty('moduleWorkflowConditionCount')) {

			console.log("its working");
			var row = $('<tr>').addClass('table-content-row');
			row.append($('<td>').text(moduleCustom.moduleType).css('text-align', 'center'));

			['moduleCustomFieldCount', 'moduleWorkflowFunctionCount', 'moduleWorkflowConditionCount'].forEach(function(countType) {
				var count = moduleCustom[countType];
				var countCell = $('<td>').css('text-align', 'center');
				if (count > 0) {
					var hyperlink = $('<a>').addClass('data-span clickable-cell')
						.text(count)
						.data('heading', moduleCustom.moduleType)
						.data('type', countType);


					countCell.append(hyperlink);
				} else {
					countCell.text(count);
				}
				row.append(countCell);
			});

			$('#userTableBodyDocument').append(row);
		}
	});
}


$(document).on('click', '.clickable-cell', function() {
	const projectId = $("#projectDropDown").val();
	alert("Selected project Id is: " + projectId);
    var heading = $(this).data('heading');
    var type = $(this).data('type');
    console.log("Heading: " + heading + "\nType: " + type);

    // Make an AJAX call
    $.ajax({
        url: `usermana?action=getCustomizationDetails&heading=${heading}&type=${type}&projectId=${projectId}`,
        method: 'GET',

        success: function(response) {
  		console.log("Response is",response)
        },
        error: function(error) {
            // Handle error
            console.error("Error Message is", error);
        }
    });
});





function showPopupForPrePost(scriptType, scriptList) {
	$('#popupModel').css('display', 'block');
	const modal = $('<div>').addClass('modal');
	const modalContent = $('<div>').addClass('modal-content');
	const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(scriptType);
	const popupBody = $('<div>').addClass('popup-body').css({
		'max-height': '300px',
		'overflow-y': 'auto'
	});
	const table = $('<table>').addClass('table-main');
	const tbody = $('<tbody>').attr('id', 'popupDetailsTable');
	const tableHeaderRow = $('<tr>').addClass('table-header-row');
	tableHeaderRow.append($('<th>').text('Script Name'));
	tbody.append(tableHeaderRow);

	// Check if the number of items in scriptList is greater than 8
	if (Object.keys(scriptList).length > 8) {
		popupBody.css('max-height', '500px'); // Adjust the max-height as needed
	}

	for (const fileName in scriptList) {
		const tableContentRow = $('<tr>').addClass('table-content-row');
		tableContentRow.append($('<td>').text(fileName));
		tbody.append(tableContentRow);
	}

	table.append(tbody);
	popupBody.append(table);
	const popupFooter = $('<div>').addClass('popup-footer');
	const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function() {
		modal.hide();
		$('#popupModel').css('display', 'none');
	});
	popupFooter.append(closeBtn);
	modalContent.append(popupHeading, popupBody, popupFooter);
	modal.append(modalContent);
	$('#pre-post').append(modal);
	modal.show();
}


