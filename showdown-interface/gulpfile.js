const del = require('del');
const gulp = require('gulp');
const $ = require('gulp-load-plugins')();
// Doesn't work through $. No idea why
const gulpSourcemaps = require('gulp-sourcemaps');

const tsProject = $.typescript.createProject("tsconfig.json");

gulp.task('clean', function() {
  return del('built/**');
});

gulp.task('build', function() {
  const tsResult = tsProject.src()
      .pipe(gulpSourcemaps.init())
      .pipe(tsProject());

  return tsResult.js
      .pipe(gulpSourcemaps.write())
      .pipe(gulp.dest('built'));
});

gulp.task('test', ['build'], function() {
  gulp.src('built/**/*_spec.js')
      .pipe($.mocha());
});

gulp.task('lint', function() {
  return gulp.src('src/**/*.ts')
      .pipe($.tslint({
          formatter: "verbose",
          configuration: ".tslint.json"
      }))
      .pipe($.tslint.report({summarizeFailureOutput: true}));
});

gulp.task('default', ['build'], function() {
  const server = $.liveServer.new('./built/index.js');
  server.start();
});
