module.exports = {
    open: function(success, failure, mimeType, allowMultipleSelection) {
      cordova.exec(success, failure, 'FileChooser', 'open', [
        mimeType || '*/*',
        allowMultipleSelection === undefined ? true : allowMultipleSelection
      ]);
    }
  };
  