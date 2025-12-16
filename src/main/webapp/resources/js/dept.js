console.log("dept.js..");
var deptSelector = "span[data-cvoc-protocol='deptMadrono']";
var deptInputSelector = "input[data-cvoc-protocol='deptMadrono']";
var deptRetrievalUrl = "https://eciencia.consorciomadrono.es/department";
var deptIdStem = "https://eciencia.consorciomadrono.es/";
var deptPrefix = "deptMadrono";
//Max chars that displays well for a child field
var deptMaxLength = 63;

$(document).ready(function() {
    expandDepts();
    updateDeptInputs();
});

function expandDepts() {
    // Check each selected element
    $(deptSelector).each(function() {
        var deptElement = this;
        // If it hasn't already been processed
        if (!$(deptElement).hasClass('expanded')) {
          //Child field case - if non-managed display, the string before this is name (affiliation) and we need to remove the duplicate affiliation string
          //This is true for Dataverse author field - may not be true elsewhere - tbd
          let prev = $(deptElement)[0].previousSibling;
          if(prev !== undefined) {
          let val = $(deptElement)[0].previousSibling.nodeValue;
            if(val !== null) {
              $(deptElement)[0].previousSibling.data = val.substring(0,val.indexOf('('));
            }
          }
            // Mark it as processed
            $(deptElement).addClass('expanded');
            var id = deptElement.textContent;
            if (!id.startsWith(deptIdStem)) {
                $(deptElement).html(getDeptDisplayHtml(id, null, ['No Entry'], false, true));
            } else {
                //Remove the URL prefix - "https://eciencia.consorciomadrono.es/".length = 36
                id = id.substring(deptIdStem.length);
                //Check for cached entry
                let value = getValue(deptPrefix, id);
                if(value.name !=null) {
                    $(deptElement).html(getDeptDisplayHtml(value.name, deptIdStem + id, value.altNames, false, true));
                } else {
                    // Try it as an department of the Consorcio Madroño entry (could validate that it has the right form or can just let the GET fail)
                    $.ajax({
                        type: "GET",
                        url: deptRetrievalUrl + "/" + id,
                        dataType: 'json',
                        headers: {
                            'Accept': 'application/json',
                        },
                        success: function(dept, status) {
                            // If found, construct the HTML for display
                            var name = dept.name;
                            var altNames= dept.acronyms;

                            $(deptElement).html(getDeptDisplayHtml(name, deptIdStem + id, altNames, false, true));
                            //Store values in localStorage to avoid repeating calls to CrossRef
                            storeValue(deptPrefix, id, name + "#" + altNames);
                        },
                        failure: function(jqXHR, textStatus, errorThrown) {
                            // Generic logging - don't need to do anything if 404 (leave
                            // display as is)
                            if (jqXHR.status != 404) {
                                console.error("The following error occurred: " + textStatus, errorThrown);
                            }
                        }
                    });
                }
            }
        }
    });
}

function getDeptDisplayHtml(name, url, altNames, truncate=true, addParens=false) {
    if(typeof(altNames) == 'undefined') {
        altNames=[];
    }
    if (truncate && (name.length >= deptMaxLength)) {
        // show the first characters of a long name
        // return item.text.substring(0,25) + "…";
        altNames.unshift(name);
        name=name.substring(0,deptMaxLength) + "…";
    }
    if(url != null) {
      name =  name + '<a href="' + url + '" target="_blank" rel="nofollow" >' +'<img alt="Consorcio Madroño logo" src="https://raw.githubusercontent.com/Consorcio-Madrono/dataverse/refs/heads/v6.5Madrono/src/main/webapp/resources/images/fav/favicon-32x32.png" height="20" class="ror"/></a>';
    }
    if(addParens) {
        name = ' (' + name + ')';
    }
    return $('<span></span>').append(name).attr("title", altNames);
}

function updateDeptInputs() {
    // For each input element within deptInputSelector elements
    $(deptInputSelector).each(function() {
        var deptInput = this;
        if (!deptInput.hasAttribute('data-dept')) {
            // Random identifier
            let num = Math.floor(Math.random() * 100000000000);
            // Hide the actual input and give it a data-dept number so we can
            // find it
            $(deptInput).hide();
            $(deptInput).attr('data-dept', num);
            // Todo: if not displayed, wait until it is to then create the
            // select 2 with a non-zero width
            // Add a select2 element to allow search and provide a list of
            // choices
            var selectId = "deptAddSelect_" + num;
            $(deptInput).after(
                '<select id=' + selectId + ' class="form-control add-resource select2" tabindex="0" >');
            $("#" + selectId).select2({
                theme: "classic",
                tags: $(deptInput).attr('data-cvoc-allowfreetext'),
                delay: 500,
                templateResult: function(item) {
                    // No need to template the searching text
                    if (item.loading) {
                        return item.text;
                    }
                    // markMatch bolds the search term if/where it appears in
                    // the result
                    var $result = markMatch2(item.text, term);
                    return $result;
                },
                templateSelection: function(item) {
                    // For a selection, format as in display mode
                    //Find/remove the id number
                    var name = item.text;
                    var pos = item.text.search(/, [a-z0-9]{9}/);
                    if (pos >= 0) {
                        name = name.substr(0, pos);
                        var idnum = item.text.substr(pos+2);
                        var altNames=[];
                        pos=idnum.indexOf(', ');
                        if(pos>0) {
                            altNames = idnum.substr(pos+2).split(',');
                            idnum=idnum.substr(0,pos);
                        }
                        return getDeptDisplayHtml(name, deptIdStem + idnum, altNames);
                    }
                    return getDeptDisplayHtml(name, null, ['No Entry']);
                },
                language: {
                    searching: function(params) {
                        // Change this to be appropriate for your application
                        return 'Search by name or acronym…';
                    }
                },
                placeholder: deptInput.hasAttribute("data-cvoc-placeholder") ? $(deptInput).attr('data-cvoc-placeholder') : "Select a research organization",
                minimumInputLength: 3,
                allowClear: true,
                ajax: {
                    // Use an ajax call to the Consorcio Madroño Departments entry point to retrieve matching results
                    url: deptRetrievalUrl,
                    data: function(params) {
                        term = params.term;
                        if (!term) {
                            term = "";
                        }
                        var query = {
                            query: term,
                        }
                        return query;
                    },
                    // request json
                    headers: {
                        'Accept': 'application/json'
                    },
                    processResults: function(data, params) {
                        //console.log("Data dump BEGIN");
                        //console.log(data);
                        //console.log("Data dump END");
                        return {
                            results: data['items']
                                // Sort the list
                                // Prioritize active orgs
                                .sort((a, b) => Number(b.status === 'active') - Number(a.status === 'active'))
                                // Prioritize those with this acronym
                                .sort((a, b) => Number(b.acronyms.includes(params.term)) - Number(a.acronyms.includes(params.term)))
                                // Prioritize previously used entries
                                .sort((a, b) => Number(getValue(deptPrefix, b['id'].replace(deptIdStem,'')).name != null) - Number(getValue(deptPrefix, a['id'].replace(deptIdStem,'')).name != null))
                                .map(
                                    function(x) {
                                        return {
                                            text: x.name +", " + x.id.replace(deptIdStem,'') + ', ' + x.acronyms,
                                            id: x.id
                                        }
                                    })
                        };
                    }
                }
            });
          //Add a tab stop and key handling to allow the clear button to be selected via tab/enter
          const observer = new MutationObserver((mutationList, observer) => {
            var button = $('#' + selectId).parent().find('.select2-selection__clear');
            console.log("BL : " + button.length);
            button.attr("tabindex","0");
            button.on('keydown',function(e) {
              if(e.which == 13) {
                $('#' + selectId).val(null).trigger('change');
              }
            });
          });

          observer.observe($('#' + selectId).parent()[0], {
            childList: true,
            subtree: true }
          );

            // If the input has a value already, format it the same way as if it
            // were a new selection
            var id = $(deptInput).val();
            if (id.startsWith(deptIdStem)) {
                id = id.substring(deptIdStem.length);
                $.ajax({
                    type: "GET",
                    url: deptRetrievalUrl + "/" + id,
                    dataType: 'json',
                    headers: {
                        'Accept': 'application/json'
                    },
                    success: function(dept, status) {
                        var name = dept.name;
                        //Display the name and id number in the selection menu
                        var text = name + ", " + dept.id.replace(deptIdStem,'') +', ' + dept.acronyms;
                        var newOption = new Option(text, id, true, true);
                        $('#' + selectId).append(newOption).trigger('change');
                    },
                    failure: function(jqXHR, textStatus, errorThrown) {
                        if (jqXHR.status != 404) {
                            console.error("The following error occurred: " + textStatus, errorThrown);
                        }
                    }
                });
            } else {
                // If the initial value is not in the Consorcio Madroño departments entry point, just display it as is
                var newOption = new Option(id, id, true, true);
                newOption.altNames = ['No Entry'];
                $('#' + selectId).append(newOption).trigger('change');
            }
            // Could start with the selection menu open
            // $("#" + selectId).select2('open');
            // When a selection is made, set the value of the hidden input field
            $('#' + selectId).on('select2:select', function(e) {
                var data = e.params.data;
                // For entries from the Consorcio Madroño departments, the id and text are different
                //For plain text entries (legacy or if tags are allowed), they are the same
                if (data.id != data.text) {
                    // we want just the dept url
                    $("input[data-dept='" + num + "']").val(data.id);
                } else {
                    // Tags are allowed, so just enter the text as is
                    $("input[data-dept='" + num + "']").val(data.id);
                }
            });
            // When a selection is cleared, clear the hidden input
            $('#' + selectId).on('select2:clear', function(e) {
                $("input[data-dept='" + num + "']").attr('value', '');
            });
            //When the field is selected via keyboard, move the focus and cursor to the new input
            $('#' + selectId).on('select2:open', function(e) {
              $(".select2-search__field").focus()
              $(".select2-search__field").attr("id",selectId + "_input")
              document.getElementById(selectId + "_input").select();

            });
        }
    });
}
