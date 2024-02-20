$(document).ready(function() {
	licenseDetails();
	$.ajax({
		url: "usermana?action=getProjectList",
		type: "GET",
		dataType: "json",
		success: function(data) {
			const projectId = data.projectMap;
			const hookMap = data.hookMap
			console.log("hookMap", hookMap);

			$('#pre-post a').on('click', function() {
				showPopup('Pre-Post Save Details', hookMap);
			});

			const selectElement = $("#projectDropDown");
			selectElement.empty();

			try {
				// Use Object.entries() to get an array of [key, value] pairs from the projectId object
				for (const [key, name] of Object.entries(projectId)) {
					const option = $("<option>").val(key).text(name);
					selectElement.append(option);
				}
			} catch (error) {
				console.error("Error processing data:", error);
			}
		},
		error: function(error) {
			console.error("Error message is", error.statusText);
		}
	});
});

 function reportInfo() {
    const projectId = $("#projectDropDown").val();
    $('.polarion-rpw-table-content').show();
    const tbody = $('#spaceReportTableBody');
    $.ajax({
        url: `usermana?action=getSpacesDetails&projectId=${projectId}`,
        type: "GET",
        dataType: "json",

        success: function(respData) {
            var spaceData = respData.data;
            var tableData = "";
            var tableBody = document.getElementById("spaceReportTableBody");
            for (var i = 0; i < spaceData.length; i++) {
                tableData += "<tr>";
                tableData += "<td>" + spaceData[i].spaceName + "</td>";

                tableData += "<td>";
                for (var j = 0; j < spaceData[i].reportDetails.length; j++) {
                    var polarionStartingUrl = "/polarion/#/project/";
                    var mainUrl;
                    var baseUrl = window.location.protocol + "//" + window.location.host;
                    var reportNames = spaceData[i].reportDetails[j].reportName;
                    if(spaceData[i].spaceName=="_default"){
				     mainUrl = baseUrl + polarionStartingUrl + projectId + "/wiki/" + reportNames;
				     }else{	
                    mainUrl = baseUrl + polarionStartingUrl + projectId + "/wiki/" + spaceData[i].spaceName + "/" + reportNames;
                    }
                    tableData += "<div style='font-size: 14px;'><a href='" + mainUrl + "' target='_blank' class='non-underline'>" + spaceData[i].reportDetails[j].reportName + "</a></div>";

                }
                
                tableData += "</td>";
                tableData += "<td>";
                for (var j = 0; j < spaceData[i].reportDetails.length; j++) {
                    tableData += "<div style='font-size: 14px;'>" + spaceData[i].reportDetails[j].createdDates + "</div>";
                }
                tableData += "</td>";
                 tableData += "<td>";
                for (var k = 0; k < spaceData[i].reportDetails.length; k++) {
                    tableData += "<div style='font-size: 14px;'>" + spaceData[i].reportDetails[k].updatedDate + "</div>";
                }
                tableData += "</td>";
                
            }
            tableBody.innerHTML = tableData;
        },
        error: function(e) {
            console.log("Error ----" + e.message);
        }
    });
}

function pluginInfo() {
    $('.polarion-rpw-table-content').show();
    $.ajax({
        url: `usermana?action=getPluginDetails`,
        type: "GET",
        dataType: "json",

        success: function(respData) {
            var pluginData = respData.data;
           
            var tableData = "";
            var tableBody = document.getElementById("pluginReportTableBody");
           
            for (var i = 0; i < pluginData.length; i++) {
                tableData += "<tr>";
                tableData += "<td>" + pluginData[i].pluginDeatils + "</td>";
                tableData += "<td>" + pluginData[i].location + "</td>";
                tableData += "</tr>";
                
            }
            
            tableBody.innerHTML = tableData;
        },
    });
}

function licenseDetails()
{
		$('.polarion-rpw-table-content').show();
		    $.ajax({
		        url: `usermana?action=getpolarionLicenseDetails`,
		        type: "GET",
		        dataType: "json",
		        
              success: function(respData) {
				  var licenseData=respData.data[0];
				  console.log("Response data"+licenseData);
				  var tableBody=document.getElementById("licenseDataTableBody");
				  var tableData = "";
				 
			    tableData += "<tr>";
                tableData += "<td><strong>User Name</strong></td>";
                tableData += "<td>" + licenseData.userName + "</td>";
                tableData += "</tr>";
                tableData += "<tr>";
                tableData += "<td><strong>License Type</strong></td>";
                tableData += "<td>" + licenseData.licenseType + "</td>";
                tableData += "</tr>"; 
                tableData += "<tr>";
                tableData += "<td><strong>User Company</strong></td>";
                tableData += "<td>" + licenseData.userCompany + "</td>";
                tableData += "</tr>";
					  
				  
			tableBody.innerHTML = tableData;
			},
			});	
}

function projectInfo() {
     reportInfo();
     pluginInfo();
     
	const projectId = $("#projectDropDown").val();
	//alert("selected project Id is:" + projectId);
	$('.polarion-rpw-table-content').show();
	const tbody = $('#userTableBody');



	$.ajax({
		url: `usermana?action=getCustomizationDetails&projectId=${projectId}`,
		type: "GET",
		dataType: "json",

		success: function(data) {

			const customDetailsList = data.customDetailsList;
			const scriptConditionList = data.scriptConditionList;
			//const scriptFunctionList = data.scriptFunctionList;
			const customEnumerationList = data.customEnumerationList;
			const scriptCountList = data.scriptCountList;
			const scriptFunctionCountList = data.scriptFunctionCountList;
			
			 $('#pre-post').show();

			console.log("scriptConditionList", scriptConditionList);
			console.log("scriptCountList", scriptCountList);
			console.log("scriptCountList", scriptFunctionCountList);
			tbody.empty();

			for (let i = 0; i < customDetailsList.length; i++) {
				console.log("CustomDetails List is:", customDetailsList);
				const row = $('<tr>');
				row.append($('<td>').text(customDetailsList[i].workItemType));
				const customFieldCell = $('<td>');
				const customFieldButton = $('<button>').text(customDetailsList[i].customFieldsCount).addClass("customField-td-button");
				customFieldCell.append(customFieldButton);
				row.append(customFieldCell);

				// Update this block to use a helper functionscriptFunctionList

				updateCellBasedOnEnumCondition(customEnumerationList, customDetailsList[i].workItemType, 'customEnumerationCount', row);
				updateCellBasedOnScriptConditionFunction(customDetailsList[i].workItemType, scriptFunctionCountList, 'scriptCount', row);
				updateCellBasedOnScriptConditionFunction(customDetailsList[i].workItemType, scriptCountList, 'scriptCount', row);
				tbody.append(row);
			}

		},

		error: function(error) {
			console.error("Error message is", error);

		}

	});
}

// Helper function to update the cell based on condition

function updateCellBasedOnEnumCondition(list, workItemType, countProperty, row) {
	const cell = $('<td>');
	const matchingItem = list.find(item => item.customWorkItemType === workItemType);
	if (matchingItem) {
		const button = $('<button>').text(matchingItem[countProperty]).addClass("customField-td-button");
		cell.append(button);
	} else {
		const button = $('<button>').text(0).addClass("customField-td-button");
		cell.append(button);
	}
	row.append(cell);
}

// Helper function to update the cell based on condition

function updateCellBasedOnScriptConditionFunction(workItemType, countList, scriptCount, row) {
	const cell = $('<td>');
	const matchingItem = countList.find(item => item.wiTypeEnum === workItemType);
	console.log("Matching Item is:", matchingItem);
	if (matchingItem) {
		const button = $('<button>').text(matchingItem[scriptCount]).addClass("customField-td-button");
		cell.append(button);
	} else {
		const button = $('<button>').text(0).addClass("customField-td-button");
		cell.append(button);
	}
	row.append(cell);

}
// Modified showPopup function
function showPopup(scriptType, scriptList) {
    // Create a modal popup
   
    $('#popupModel').css('display', 'block');
    const modal = $('<div>').addClass('modal');
    const modalContent = $('<div>').addClass('modal-content');
    const popupHeading = $('<h4>').addClass('popup-heading').attr('id', 'popupHeading').text(scriptType);

    // Create the body of the modal
    const popupBody = $('<div>').addClass('popup-body');
    const table = $('<table>').addClass('table-main');
    const tbody = $('<tbody>').attr('id', 'popupDetailsTable');

    // Add table headers
    const tableHeaderRow = $('<tr>').addClass('table-header-row');
    tableHeaderRow.append($('<th>').text('Script Name'));
    tbody.append(tableHeaderRow);

    // Add table content
    for (const fileName in scriptList) {
        const tableContentRow = $('<tr>').addClass('table-content-row');
        tableContentRow.append($('<td>').text(fileName));
        tbody.append(tableContentRow);
    }

    table.append(tbody);
    popupBody.append(table);

    // Create the footer of the modal
    const popupFooter = $('<div>').addClass('popup-footer');
    const closeBtn = $('<span>').addClass('btn-popup-close').text('Close').on('click', function () {
        modal.hide();
        $('#popupModel').css('display', 'none');
    });

    popupFooter.append(closeBtn);
    // Assemble the modal content
    modalContent.append(popupHeading, popupBody, popupFooter);
    modal.append(modalContent);

    // Append the modal to the body
    $('#pre-post').append(modal);

    // Show the modal
    modal.show();
}

