const gulp = require('gulp');
const $ = require('gulp-load-plugins')();

gulp.task('test', function() {
  gulp.src('**/*_spec.js')
      .pipe($.mocha());
});

gulp.task('lint', function() {
  return gulp.src('**/*.js')
      .pipe($.eslint())
      .pipe($.eslint.format())
      .pipe($.eslint.failAfterError());
});

gulp.task('default', function() {
  const server = $.liveServer.new('index.js');
  server.start();
});
