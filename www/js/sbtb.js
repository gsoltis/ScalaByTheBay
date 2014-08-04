WebsocketConnection = function(url, onOpen, onMessage, onClose, onError) {
  this.url = url;
  this.onOpen = onOpen;
  this.onMessage = onMessage;
  this.onClose = onClose;
  this.onError = onError;
  this.ws_ = null;
  this.open_();
};

WebsocketConnection.prototype.open_ = function() {
  this.ws_ = new WebSocket(this.url);
  var self = this;
  this.ws_.onopen = function() {
    self.onOpen();
  };
  this.ws_.onmessage = function(msg) {
    self.onMessage(msg.data);
  };
  this.ws_.onclose = function() {
    self.onClose();
  };
  this.ws_.onerror = function(err) {
    self.onError(err.data || err.message);
  };
};

WebsocketConnection.prototype.send = function(msg) {
  this.ws_.send(msg);
};