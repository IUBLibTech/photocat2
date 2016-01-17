  // Used as an associative array to keep track of all the id
  // values for text boxes that already have an instance of
  // Autocompleter.Extended.
  loadedAutoCompleters = new Object();
  
  function instantiateAutocompleter(textBox, divPrefix, sourceIdToken, sourceIdReplacement, targetName, actionUrl, parameters) {
      if (loadedAutoCompleters[textBox.id] != 'true') {
          loadedAutoCompleters[textBox.id] = 'true';
          sourceId = textBox.id.replace(sourceIdToken, sourceIdReplacement);
          //alert("Instantiating new Autocompleter.Extended('" + textBox.id + "', '" + divPrefix + textBox.id + "', '" + sourceId + " (" + textBox.id + ".replace(" + sourceIdToken + ", " + sourceIdReplacement +  ")', '" + targetName + "', '" + actionUrl + "', " + parameters + ");");
          new Autocompleter.Extended(textBox.id, divPrefix + textBox.id, sourceId, targetName, actionUrl, parameters);
      }
  }

Autocompleter.Extended = Class.create(Autocompleter.Base, {
  initialize: function(element, update, source, target, url, options) {
    this.baseInitialize(element, update, options);
    this.options.asynchronous  = true;
    this.options.onComplete    = this.onComplete.bind(this);
    this.options.defaultParams = this.options.parameters || null;
    this.options.paramName     = target;
    this.url                   = url;
    sourceElement              = $(source);
    this.sourceElement         = sourceElement;
  },

  getUpdatedChoices: function() {
    this.startIndicator();

    var entry = encodeURIComponent(this.options.paramName) + '=' +
      encodeURIComponent(this.getToken());

    this.options.parameters = this.options.callback ?
      this.options.callback(this.element, entry) : entry;

    if(this.options.defaultParams)
      this.options.parameters += '&' + this.options.defaultParams;

   if (this.sourceElement.value) {
      this.options.parameters += '&' + this.options.paramName + '_source=' + this.sourceElement.value;
   }

    new Ajax.Request(this.url, this.options);
  },

  onComplete: function(request) {
    this.updateChoices(request.responseText);
  }
});
