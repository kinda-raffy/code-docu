const path = require('path')

module.exports = {
	entry: './CodeDocu/codedocu.js',
	output: {
		filename: 'worker.js',
		path: path.resolve(__dirname, 'dist'),
		library: {
			type: 'module',
		},
	},
	experiments: {
		outputModule: true,
	 },
	mode: 'production',
};

