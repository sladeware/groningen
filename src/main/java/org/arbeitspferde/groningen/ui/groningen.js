/* Copyright 2012 Google, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


/**
 * @fileoverview Groningen Dashboard client.
 */


/** Namespace */
var groningen = {};

/**
 * Handlebars template for pipeline listing
 * @private
 * @type {function}
 */
groningen.pipelineGroupTemplate_ = null;

/**
 * Handlebars template for detailed pipeline information
 * @private
 * @type {function}
 */
groningen.pipelineTemplate_ = null;

/**
 * Handlebars template for pipeline content only.
 * @private
 * @type {function}
 */
groningen.pipelineContentTemplate_ = null;

/**
 * Handlebars template for graph datapoint tooltip.
 * @private
 * @type {function}
 */
groningen.graphPointTooltipTemplate_ = null;

/**
 * Returns Handlebars template for pipeline listing.
 * @private
 * @return {function} Handlebars template.
 */
groningen.getPipelineGroupTemplate_ = function() {
  if (groningen.pipelineGroupTemplate_ == null) {
    var source = $('#pipeline-group-template').html();
    groningen.pipelineGroupTemplate_ = Handlebars.compile(source);
  }
  return groningen.pipelineGroupTemplate_;
};

/**
 * Returns Handlebars template for pipeline.
 * @private
 * @return {function} Handlebars template.
 */
groningen.getPipelineTemplate_ = function() {
  if (groningen.pipelineTemplate_ == null) {
    var source = $('#pipeline-template').html();
    groningen.pipelineTemplate_ = Handlebars.compile(source);
  }
  return groningen.pipelineTemplate_;
};

/**
 * Returns Handlebars template for pipeline content.
 * @private
 * @return {function} Handlebars template.
 */
groningen.getPipelineContentTemplate_ = function() {
  if (groningen.pipelineContentTemplate_ == null) {
    var source = $('#pipeline-content-partial').html();
    groningen.pipelineContentTemplate_ = Handlebars.compile(source);
  }
  return groningen.pipelineContentTemplate_;
};

/**
 * Returns Handlebars template for graph datapoint tooltip.
 * @private
 * @return {function} Handlebars template.
 */
groningen.getGraphPointTooltipTemplate_ = function() {
  if (groningen.graphPointTooltipTemplate_ == null) {
    var source = $('#graph-score-tooltip').html();
    groningen.graphPointTooltipTemplate_ = Handlebars.compile(source);
  }
  return groningen.graphPointTooltipTemplate_;
};

/**
 * Attach event handlers to dashboard components.
 */
groningen.attachEventHandlers = function() {
  // Filter as you type.
  $('#userfilter').keyup(groningen.filterPipelineList_);
  // Expand All / Collapse All
  $('.expandlabel').click(function() {
    $('.expandlabel').toggle();
    $('#pipelinelist div.collapse').collapse($(this).attr('data-action'));
  });
  // Fetch details about pipeline.
  $('body').on('click', '#pipelinelist dl[data-pipelineid]', function() {
    groningen.renderCurrentPipeline_($(this).attr('data-pipelineid'));
  });
  // Pin a pipeline
  $('body').on('click', '.breadcrumb > .pin', function(eventObj) {
    $(this).parents('.pipeline-box')
        .appendTo('#pinned')
        .find('.action-button:not(.refresh)')
        .toggle();
    groningen.updateLocationHash_();
    eventObj.stopPropagation();
  });
  // Remove a pipeline
  $('body').on('click', '.breadcrumb > .remove', function() {
    $(this).parents('.pipeline-box').remove();
    groningen.updateLocationHash_();
  });
  // Refresh a pipeline
  $('body').on('click', '.breadcrumb > .refresh', function(eventObj) {
    eventObj.stopPropagation();
    var pipelineContainer = $(this).parents('.pipeline-box');
    pipelineContainer.attr('disabled', 'true')
        .children('div.collapse')
        .collapse('hide')
        .remove();
    var pipelineId = pipelineContainer.attr('data-pipelineid');
    groningen.fetchPipelines_(pipelineId, function(pipelineContainer) {
      return function(data) {
      groningen.renderPipelineContent_(data[0], pipelineContainer);
      pipelineContainer.attr('disabled', false);
      };
    }(pipelineContainer));
  });
};

/**
 * Event handler for pipelinelist form. Filters the list as user types.
 * @private
 */
groningen.filterPipelineList_ = function() {
  var typed_text = $('#userfilter').val();
  $('#pipelinelist > li').each(function() {
    var user = $(this).find('a').text();
    user.indexOf(typed_text) == -1 ? $(this).hide() : $(this).show();
  });
};

/**
 * Updates the pipeline listing using data from REST endpoint.
 */
groningen.updatePipelineList = function() {
  $.getJSON('/groningen/pipelines', function(data) {
    var indexArray = [];
    var groupedData = [];
    $.each(data, function(_, value) {
      var username = value.user;
      var pipelineGroup = indexArray[username];
      if (pipelineGroup == undefined) {
        pipelineGroup = {
          user: username,
          pipelines: []
        };
        indexArray[username] = pipelineGroup;
        groupedData.push(pipelineGroup);
      }
      pipelineGroup.pipelines.push(value);
    });
    var template = groningen.getPipelineGroupTemplate_();
    $('#pipelinelist').empty().append(template(groupedData));
  });
};

/**
 * Renders pipeline information when a listing is clicked.
 * @private
 * @param {string} pipelineId Pipeline ID.
 */
groningen.renderCurrentPipeline_ = function(pipelineId) {
  $('.pipeline-box[data-pipelineid!=' + pipelineId + '] > div.collapse')
      .collapse('hide');
  var pipelineSelector = $('.pipeline-box[data-pipelineid=' + pipelineId + ']');
  if (pipelineSelector.length > 0) {
    pipelineSelector.children('div.collapse').collapse('show');
    return;
  }
  $('#current').empty();
  groningen.fetchPipelines_(pipelineId, function(data) {
    $('#current').empty();
    groningen.renderPipeline_(data[0], '#current');
  });
};

/**
 * Loads pipeline in the pinned section using pipeline IDs from URL.
 */
groningen.loadPinnedPipelines = function() {
  var hash = window.location.hash;
  var pipelineIds = hash.length > 1 ? hash.substring(1) : '';
  if (pipelineIds != '') {
    groningen.fetchPipelines_(pipelineIds, function(data) {
      $.each(data, function(_, dataItem) {
        groningen.renderPipeline_(dataItem, '#pinned');
      });
      $('#pinned .action-button:not(.refresh)').toggle();
      $('#pinned .pipeline-box > div.collapse').collapse('hide');
    });
  }
};

/**
 * Fetches details about pipelines and renders them using specified function.
 * @private
 * @param {string} pipelineIds Comma separated list of pipeline IDs.
 * @param {function} renderingFunction Function for rendering the pipeline
 *     information.
 */
groningen.fetchPipelines_ = function(pipelineIds, renderingFunction) {
  $.getJSON('/groningen/pipelines/' + pipelineIds, renderingFunction);
};

/**
 * Renders detailed pipeline information inside given div.
 * @private
 * @param {DetailedPipelineInfo} pipelineData JSON representation of detailed
 *     information about pipeline.
 * @param {string} parentSelector jQuery selector for parent div which will hold
 *     the rendered elements.
 */
groningen.renderPipeline_ = function(pipelineData, parentSelector) {
  var template = groningen.getPipelineTemplate_();
  groningen.renderPipelineWithTemplate_(pipelineData, parentSelector, template);
};

/**
 * Renders the content section of the pipeline.
 * @private
 * @param {DetailedPipelineInfo} pipelineData JSON representation of detailed
 *     information about the pipeline.
 * @param {string} parentSelector jQuery selector for parent div which will hold
 *     the rendered elements.
 */
groningen.renderPipelineContent_ = function(pipelineData, parentSelector) {
  var template = groningen.getPipelineContentTemplate_();
  groningen.renderPipelineWithTemplate_(pipelineData, parentSelector, template);
};

/**
 * Renders pipeline data using a given Handlebars template
 * @private
 * @param {DetailedPipelineInfo} pipelineData JSON representation of detailed
 *     information about the pipeline.
 * @param {string} parentSelector jQuery selector for parent div which will hold
 *     the rendered elements.
 * @param {function} template Handlebars template.
 */
groningen.renderPipelineWithTemplate_ = function(
    pipelineData, parentSelector, template) {
  $(template(pipelineData))
      .appendTo(parentSelector)
      .find('[rel=tooltip]')
      .tooltip({placement: 'right'});
  var historyData = [];
  $.each(pipelineData.history, function(_, element) {
    historyData.push([element.gen, element.score, element.settings]);
  });
  groningen.renderGraph_(pipelineData.pipelineId, historyData);
};

/**
 * Renders graph in the history tab of the detailed pipeline information.
 * @private
 * @param {string} pipelineId ID of the pipeline, used to generate jQuery
 *     selectors.
 * @param {Array} data History data to be rendered on graph.
 */
groningen.renderGraph_ = function(pipelineId, data) {
  var primarySelector = '#' + pipelineId + '_tab2 .primary-graph';
  var secondarySelector = '#' + pipelineId + '_tab2 .secondary-graph';
  var firstTabSelector = 'a[href=#' + pipelineId + '_tab1]';
  var maxGeneration = -1;
  var maxScore = -1.0;
  $.each(data, function(_, element) {
    maxGeneration = Math.max(element[0], maxGeneration);
    maxScore = Math.max(element[1], maxScore);
  });
  /* Show atleast 10 ticks on the axis */
  maxGeneration = Math.max(maxGeneration, 10);
  var detailed = $.plot($(primarySelector), [data], {
    series: {points: {show: true}},
    xaxis: {tickSize: 1, min: 0, max: 10, panRange: [0, maxGeneration + 1]},
    yaxis: {panRange: false, min: 0, max: maxScore},
    pan: {interactive: true},
    grid: {hoverable: true}
  });
  var overview = $.plot($(secondarySelector), [data], {
    series: {points: {show: true, radius: 1}},
    xaxis: {ticks: [], min: 0, max: maxGeneration + 1},
    yaxis: {ticks: [], min: -10, max: maxScore + 10},
    selection: {mode: 'x'}
  });
  overview.setSelection({xaxis: {from: 0, to: 10}});
  $(primarySelector).on('plotpan', function(e, p) {
    xaxes = p.getXAxes()[0];
    overview.setSelection({xaxis: {from: xaxes.min, to: xaxes.max}});
  });
  $(primarySelector).on('plothover', function(_, __, item) {
    if (item) {
      if (previousPoint != item.datapoint) {
        $('.graph-point-tooltip').remove();
        var template = groningen.getGraphPointTooltipTemplate_();
        $(template({
          score: item.datapoint[1].toFixed(2),
          settings: item.series.data[item.dataIndex][2]
        })).appendTo('body')
           .css({top: item.pageY + 5, left: item.pageX + 5});
        previousPoint = item.datapoint;
      }
    }
    else {
      $('.graph-point-tooltip').remove();
      previousPoint = null;
    }
  });
  /* N.B.(sanragsood): Flot library uses dummy div elements to determine the
   * dimensions of canvas. This essentially implies that the graph tab should
   * not have display set as none. We start with History tab as the active tab
   * and immediately switch to scores tab once the graph rendering is complete.
   */
  $(firstTabSelector).tab('show');
};

/**
 * Handlebars initializations
 */
groningen.initHandlebars = function() {
  Handlebars.registerPartial('scores-table', $('#scores-table-partial').html());
  Handlebars.registerPartial(
      'pipeline-content', $('#pipeline-content-partial').html());
};

/**
 * Updates window.location.hash with pipeline IDs of pinned pipelines.
 * @private
 */
groningen.updateLocationHash_ = function() {
  var pins = [];
  $('#pinned > .pipeline-box').each(function(_, __) {
    pins.push($(this).attr('data-pipelineid'));
  });
  window.location.hash = pins.join(',');
};

