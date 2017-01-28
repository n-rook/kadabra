'use strict';

const gulp = require('gulp');
const $ = require('gulp-load-plugins')();

gulp.task('default', function() {
  const server = $.liveServer.new('index.js');
  server.start();
});
