requirejs.config({
	urlArgs: function (id, url) {
		for (var i = 0; i < manifest.files.length; i++) {
			var file = manifest.files[i];
			if (file.path == url) {
				return "?version=" + file.modified;
			}
		}
		return "";
	}
});

requirejs(["Ld41Js"]);