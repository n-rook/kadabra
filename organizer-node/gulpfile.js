const del = require('del');
const gulp = require('gulp');
const $ = require('gulp-load-plugins')();


const tsProject = $.typescript.createProject("tsconfig.json");

gulp.task('clean', function() {
  return del('built/**');
});

gulp.task('build', function() {
  return tsProject.src()
        .pipe(tsProject())
        .js.pipe(gulp.dest("built"));
});

gulp.task('test', function() {
  gulp.src('built/**/*_spec.js')
      .pipe($.mocha());
});

gulp.task('lint', function() {
  return gulp.src('**/*.js')
      .pipe($.eslint())
      .pipe($.eslint.format())
      .pipe($.eslint.failAfterError());
});

gulp.task('default', ['build'], function() {
  const server = $.liveServer.new('./built/index.js');
  server.start();
});
