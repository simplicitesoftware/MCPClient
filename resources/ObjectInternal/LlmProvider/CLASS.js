/**
 * JS Class LlmProvider with front hooks
 *
 * - extends by default 'Simplicite.UI.BusinessObject' and can be changed with any inherited object class
 * - 'Simplicite.UI.BusinessObject' extends 'Simplicite.Ajax.BusinessObject': native methods to access data can be overrided
 * - do not call 'super' method to explicitly override the default behavior
 * - unimplemented hooks can be removed
 * - add temporarily 'debugger' in your code to debug within your browser console
 *
 * @class
 */
Simplicite.UI.BusinessObjects.LlmProvider = class extends Simplicite.UI.BusinessObject {

// ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ
// ZZZ For lisibility of your code, keep *only* the hooks you need and remove ZZZ
// ZZZ the empty ones (the ones that only call the super hooks)               ZZZ
// ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ

/**
 * Front hook when object is instantiated.
 * Useful to override locals (cloned from Simplicite.UI.Globals) properties before usage.
 * @param locals UI locals properties (shorthand to this.locals.ui)
 * @function
 */
onLoad(locals) {
	// empty by default
	super.onLoad(locals);
}

/**
 * Front hook before loading form data
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Form parameters
 * @function
 */
beforeLoadForm(ctn, obj, p) {
	// empty by default
	super.beforeLoadForm(ctn, obj, p);
}

/**
 * Front hook when record is not found (default NO_ROW_FOUND alert + list redirection)
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param rowId Object row ID
 * @function
 */
noRowFound(ctn, obj, rowId) {
	// default implementation
	super.noRowFound(ctn, obj, rowId);
}

/**
 * Front hook before rendering form
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Form parameters
 * @function
 */
preLoadForm(ctn, obj, p) {
	// empty by default
	super.preLoadForm(ctn, obj, p);
}

/**
 * Front hook to display the form
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Form parameters
 * @param cbk callback when rendered
 * @function
 */
displayForm(ctn, obj, p, cbk) {
	// default implementation
	super.displayForm(ctn, obj, p, cbk);
}

/**
 * Front hook when object form is loaded
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Form parameters
 * @function
 */
onLoadForm(ctn, obj, p) {
	// empty by default
	super.onLoadForm(ctn, obj, p);
}

/**
 * Front hook when object form is unloaded
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Form parameters
 * @function
 */
onUnloadForm(ctn, obj, p) {
	// empty by default
	super.onUnloadForm(ctn, obj, p);
}

/**
 * Front hook before loading list data
 * @param ctn List container
 * @param obj Object (same as this)
 * @param p List parameters
 * @function
 */
beforeLoadList(ctn, obj, p) {
	// empty by default
	super.beforeLoadList(ctn, obj, p);
}

/**
 * Front hook before rendering list
 * @param ctn List container
 * @param obj Object (same as this)
 * @param p List parameters
 * @function
 */
preLoadList(ctn, obj, p) {
	// empty by default
	super.preLoadList(ctn, obj, p);
}

/**
 * Front hook to display the list
 * @param ctn List container
 * @param obj Object (same as this)
 * @param p List parameters
 * @param cbk callback when rendered
 * @function
 */
displayList(ctn, obj, p, cbk) {
	// default implementation
	super.displayList(ctn, obj, p, cbk);
}

/**
 * Front hook to display one list record
 * @param ctn List container
 * @param row Row container (tr or div)
 * @param obj Object (same as this)
 * @param id Row ID
 * @param p List parameters
 * @param cbk callback when rendered
 * @function
 */
displayListRow(ctn, row, obj, id, p, cbk) {
	// default implementation
	super.displayListRow(ctn, row, obj, id, p, cbk);
}

/**
 * Front hook when a list row is displayed
 * @param ctn List container
 * @param obj Object (same as this)
 * @param id Row ID
 * @param item Row item
 * @param row Row container (tr or div)
 * @function
 */
onLoadListRow(ctn, obj, id, item, row) {
	// empty by default
	super.onLoadListRow(ctn, obj, id, item, row);
}

/**
 * Front hook when a list row is unloaded
 * @param ctn List container
 * @param obj Object (same as this)
 * @param id Row ID
 * @param item Row item
 * @param row Row container (tr or div)
 * @function
 */
onUnloadListRow(ctn, obj, id, item, row) {
	// empty by default
	super.onUnloadListRow(ctn, obj, id, item, row);
}

/**
 * Front hook when object list is loaded
 * @param ctn List container
 * @param obj Object (same as this)
 * @param p List parameters
 * @function
 */
onLoadList(ctn, obj, p) {
	// empty by default
	super.onLoadList(ctn, obj, p);
}

/**
 * Front hook when object list is unloaded
 * @param ctn List container
 * @param obj Object (same as this)
 * @param p List parameters
 * @function
 */
onUnloadList(ctn, obj, p) {
	// empty by default
	super.onUnloadList(ctn, obj, p);
}

/**
 * Front hook before loading search form
 * @param ctn Search container
 * @param obj Object (same as this)
 * @param p Search parameters
 * @function
 */
beforeLoadSearch(ctn, obj, p) {
	// empty by default
	super.beforeLoadSearch(ctn, obj, p);
}

/**
 * Front hook to display the search form
 * @param ctn Search container
 * @param obj Object (same as this)
 * @param p Search parameters
 * @param cbk callback when rendered
 * @function
 */
displaySearch(ctn, obj, p, cbk) {
	// default implementation
	super.displaySearch(ctn, obj, p, cbk);
}

/**
 * Front hook when object search is loaded
 * @param ctn Search container
 * @param obj Object (same as this)
 * @param p Search parameters
 * @function
 */
onLoadSearch(ctn, obj, p) {
	// empty by default
	console.log("onLoadSearch");
	super.onLoadSearch(ctn, obj, p);
}

/**
 * Front hook when object search is unloaded
 * @param ctn Search container
 * @param obj Object (same as this)
 * @param p Search parameters
 * @function
 */
onUnloadSearch(ctn, obj, p) {
	// empty by default
	super.onUnloadSearch(ctn, obj, p);
}

/**
 * Front hook before loading summary
 * @param ctn Form container
 * @param obj Object (same as this)
 * @param p Parameters
 * @function
 */
beforeLoadSummary(ctn, obj, p) {
	// empty by default
	super.beforeLoadSummary(ctn, obj, p);
}

/**
 * Front hook to display the object summary
 * @param ctn Summary container
 * @param mo Meta object
 * @param obj Object (same as this)
 * @param cbk callback when rendered
 * @function
 */
displaySummary(ctn, mo, obj, cbk) {
	// default implementation
	super.displaySummary(ctn, mo, obj, cbk);
}

/**
 * Front hook when object summary is loaded
 * @param ctn Summary container
 * @param mo Meta object
 * @param obj Object (same as this)
 * @param p Parameters
 * @function
 */
onloadSummary(ctn, mo, obj, p) {
	// empty by default
	super.onloadSummary(ctn, mo, obj, p);
}

/**
 * Front hook before loading calendar
 * @param ctn container
 * @param obj Object (same as this)
 * @param agd Agenda definition
 * @param p parameters
 * @function
 */
beforeLoadAgenda(ctn, obj, agd, p) {
	// empty by default
	super.beforeLoadAgenda(ctn, obj, agd, p);
}

/**
 * Front hook when calendar is loaded
 * @param ctn container
 * @param obj Object (same as this)
 * @param agd Agenda definition
 * @param p parameters
 * @function
 */
onLoadAgenda(ctn, obj, agd, p) {
	// empty by default
	super.onLoadAgenda(ctn, obj, agd, p);
}

/**
 * Front hook when calendar is unloaded
 * @param ctn container
 * @param obj Object (same as this)
 * @param agd Agenda definition
 * @param p parameters
 * @function
 */
onUnloadAgenda(ctn, obj, agd, p) {
	// empty by default
	super.onUnloadAgenda(ctn, obj, agd, p);
}

/**
 * Front hook before loading timesheet
 * @param ctn container
 * @param obj Object (same as this)
 * @param p parameters
 * @function
 */
beforeLoadTimesheet(ctn, obj, p) {
	// empty by default
	super.beforeLoadTimesheet(ctn, obj, p);
}

/**
 * Front hook when timesheet is loaded
 * @param ctn container
 * @param obj Object (same as this)
 * @param ts Timesheet definition
 * @function
 */
onLoadTimesheet(ctn, obj, ts) {
	// empty by default
	super.onLoadTimesheet(ctn, obj, ts);
}

/**
 * Front hook when timesheet is unloaded
 * @param ctn container
 * @param obj Object (same as this)
 * @param ts Timesheet definition
 * @function
 */
onUnloadTimesheet(ctn, obj, ts) {
	// empty by default
	super.onUnloadTimesheet(ctn, obj, ts);
}

getUserParametersForm(providersParams){
	const grantLang = $grant.lang == 'FRA'?'FRA':'ENU';
	let form = document.createElement('form');
	form.id = "llmParamsForm";
	console.log("getUserParameters");
	let context = this.item.llmPrvUserParameters;
	if (!context) return form;

	for (const [key, value] of Object.entries(context)) {
		let label = value.label[grantLang];
		let help = value.help[grantLang];
		let inputValue = providersParams ? providersParams[key] : value.default;
		console.log(inputValue, " : ", value.default);
		if (!inputValue) inputValue = value.default;

		let inputElement = $tools.input({
			type: 'number',        // Type de l'input
			id: key,              // ID de l'input
			value: inputValue,     // Valeur de l'input
			autocomplete: 'off',   // Désactiver l'autocomplétion
			class: "form-control js-focusable", //class
			style:"width:auto;",
			step:0.1,
			min:value.min,
			max:value.max,
			onchange:"let min = "+value.min+";let max = "+value.max+";if (this.value < min) {this.value = min;} else if (this.value > max) {this.value = max;}console.log(min,\": \",max,\" : \",this.value);"
		});
		let inputContainer = $("<div>",{
			class: 'field-container flex-nowrap',        // Type de l'input
		});
		inputContainer.append(inputElement);
		let helpButton = $tools.buttonHelp(key, help, label,$('<span class="btn-help"/>'));
		helpButton.class=
		inputContainer.append(helpButton);
		let group = $tools.formGroup("control-group", label, inputContainer, null, null);

		form.appendChild(group[0]);
	}
	console.log("sonar verif 2",form);
	return form;
}


getUserParameters(){
	console.log("getuserparams");
	let params =this.item.llmPrvUserParameters;
	let defaultValue ={};
	for (const [key, value] of Object.entries(params)) {
		defaultValue[key] = value.default;
	}
	return defaultValue;
}

}; // class Simplicite.UI.BusinessObjects.LlmProvider