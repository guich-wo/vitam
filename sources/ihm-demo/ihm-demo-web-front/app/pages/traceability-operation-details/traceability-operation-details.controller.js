/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

// Define controller for traceability.operation.details
angular.module('traceability.operation.details')
  .controller('traceabilityOperationDetailsController', function($scope, $routeParams, traceabilityOperationDetailsService,
    traceabilityOperationResource, responseValidator) {

      // Traceability operation to check
      $scope.traceabilityOperationId = $routeParams.operationId;

      // ********** Get traceability operation details ****** //
      var successCallback = function(response) {
        if(!responseValidator.validateReceivedResponse(response)){
          // Display error
        } else{
          // Display operation details
          var receivedDetails = response.data.$results[0].events[response.data.$results[0].events.length - 1].evDetData;
          var details = JSON.parse(receivedDetails);

          $scope.startDate = details.StartDate;
          $scope.endDate = details.EndDate;
          $scope.numberOfElement = details.NumberOfElement;

          // TODO : change this value by the stored value
          $scope.digestAlgorithm = 'SHA256';
          $scope.fileName = details.FileName;
          $scope.fileSize = details.Size; // TODO Add unit size
          $scope.hash = details.Hash;
          $scope.timeStampToken = details.TimeStampToken;
        }
      };

      var errorCallback = function(error) {
        // TODO : update error management
        console.log(error);
      };


      // ************ Verification process ****************** //
      $scope.showCheckReport = false;
      var successCheckOperationCallback = function(response) {
        if(!responseValidator.validateReceivedResponse(response)){
          // Display error
          console.log(error);
          $scope.showCheckReport = false;
        } else {
          // Display operation details
          $scope.showCheckReport = true;
          $scope.reports = response.data.$results;
        }
      };

      $scope.runTraceabilityVerificationProcess = function() {
        traceabilityOperationDetailsService.runTraceabilityVerificationProcess($scope.traceabilityOperationId, successCheckOperationCallback);
      };

      // Start by getting details for the selected operation
      traceabilityOperationDetailsService.getDetails($scope.traceabilityOperationId, successCallback, errorCallback);

  });
