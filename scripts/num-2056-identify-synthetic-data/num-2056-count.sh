#!/bin/bash

### Note install jq ibrary from here https://stedolan.github.io/jq/download/

fhirBridgeBaseURL=http://localhost:8888/fhir-bridge
#https://fhir-bridge.crr.pre-prod.num-codex.de/fhir-bridge
openEHRRestAPIBase=http://localhost:8080/ehrbase/rest/openehr/v1
#ehrbase.crr.pre-prod.num-codex.de/ehrbase/rest/openehr/v1
ehrbaseUsername=myUser
#ehrbaseAuth
ehrbasePassword=myPassword432
#brigand_OUTWARD*shaken0hydro5bumpy2braze1graffito_warlord8voguish
pseudonyms=codex_6348Q7_test03,codex_WX6QAM,codex_NC2PG0
subjectNamespace="fhir-bridge"
patientURL=${fhirBridgeBaseURL}/fhir/Patient
observationURL=${fhirBridgeBaseURL}/fhir/Observation
conditionURL=${fhirBridgeBaseURL}/fhir/Condition
procedureURL=${fhirBridgeBaseURL}/fhir/Procedure
diagnosticReportURL=${fhirBridgeBaseURL}/fhir/DiagnosticReport
procedureReportURL=${fhirBridgeBaseURL}/fhir/Procedure
medicationStatementURL=${fhirBridgeBaseURL}/fhir/MedicationStatement
immunizationURL=${fhirBridgeBaseURL}/fhir/Immunization
aqlQueryURL=${openEHRRestAPIBase}/query/aql
logToFile=true

function logInfo() {
	local identifier=$1
	local logEntry=$2
	if [ "$logToFile" = true ]; then
		echo $logEntry >> $identifier.log
	else
		echo $logEntry;
	fi	
}

for codexIdentifier in ${pseudonyms//,/ }
do
echo "start process pseudonym $codexIdentifier"
retrieveEHRBySubjectId="${openEHRRestAPIBase}/ehr?subject_id=$codexIdentifier&subject_namespace=$subjectNamespace"
aqlQueryBody="{ \"q\": \"Select c/uid/value, e/ehr_id/value, c/archetype_details/template_id/value, c/feeder_audit from EHR e contains composition c WHERE e/ehr_status/subject/external_ref/id/value = '$codexIdentifier'\" }"
#echo $retrieveEHRBySubjectId
# note fhir_resource_id is the id from fhir_bridge DB, table fb_resource_composition.resource_id

#Get patient resource id (the one stored in fhir-bridge)
resourceId=$(curl -X GET "${patientURL}?identifier=${codexIdentifier}" | jq '.entry[0].resource.id')
#if [ -z "${resourceId}"]; 
#then
#		echo "No patient found for ${codexIdentifier}"
#		continue
#fi
logEntry="patient with pseudonym $codexIdentifier has fhir_resource_id $resourceId;"
logInfo "$codexIdentifier" "$logEntry"
#Get/find corresponding EHR id for patient
ehrId=$(curl -u "$ehrbaseUsername:$ehrbasePassword" -X GET "$retrieveEHRBySubjectId" | jq '.ehr_id.value');
logEntry="Patient with pseudonym $codexIdentifier has EHR with id $ehrId";
logInfo "$codexIdentifier" "$logEntry"

findObservationResponse=$(curl -X GET "$observationURL?subject.identifier=$codexIdentifier");
logEntry=$(echo "total number of observations for patient $codexIdentifier is:" $(echo $findObservationResponse | jq '.total'))
logInfo "$codexIdentifier" "$logEntry"

logEntry=$(echo $findObservationResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id, profile: .resource.meta.profile[0]}')
logInfo "$codexIdentifier" "$logEntry"

findConditionsResponse=$(curl -X GET "$conditionURL?subject.identifier=$codexIdentifier");
logEntry=$(echo "total number of conditions for patient $codexIdentifier is:" $(echo $findConditionsResponse| jq '.total'))
logInfo "$codexIdentifier" "$logEntry"

logEntry=$(echo $findConditionsResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id}')
logInfo "$codexIdentifier" "$logEntry"

findDiagnosticReportsResponse=$(curl -X GET "$diagnosticReportURL?subject.identifier=$codexIdentifier");
logEntry=$(echo "total number of diagnostic reports for patient $codexIdentifier is:" $(echo $findDiagnosticReportsResponse| jq '.total'))
logInfo "$codexIdentifier" "$logEntry"
logEntry=$(echo $findDiagnosticReportsResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id}')
logInfo "$codexIdentifier" "$logEntry"

findProceduresResponse=$(curl -X GET "$procedureReportURL?subject.identifier=$codexIdentifier");
logEntry=$(echo "total number of procedures for patient $codexIdentifier is:" $(echo $findProceduresResponse| jq '.total'))
logInfo "$codexIdentifier" "$logEntry"
logEntry=$(echo $findProceduresResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id}')
logInfo "$codexIdentifier" "$logEntry"

findMedicationsResponse=$(curl -X GET "$medicationStatementURL?subject.identifier=$codexIdentifier");
logEntry=$(echo "total number of medications for patient $codexIdentifier is:" $(echo $findMedicationsResponse| jq '.total'))
logInfo "$codexIdentifier" "$logEntry"
logEntry=$(echo $findMedicationsResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id}')
logInfo "$codexIdentifier" "$logEntry"

findImmunizationsResponse=$(curl -X GET "$immunizationURL?patient.identifier=$codexIdentifier");
logEntry=$(echo "total number of immunizations for patient $codexIdentifier is:" $(echo $findImmunizationsResponse| jq '.total'))
logInfo "$codexIdentifier" "$logEntry"
logEntry=$(echo $findImmunizationsResponse | jq '.entry[]? | {fullUrl: .fullUrl, fhir_resource_id: .resource.id}')
logInfo "$codexIdentifier" "$logEntry"

selectCompositionsResponse=$(curl -X POST "$aqlQueryURL" -u "$ehrbaseUsername:$ehrbasePassword" -H "Content-Type: application/json" -d "${aqlQueryBody}");
logEntry=$(echo "total number of compositions for pseudonym $codexIdentifier is:" $(echo $selectCompositionsResponse| jq '.rows | length'))
logInfo "$codexIdentifier" "$logEntry"
logEntry=$(echo $selectCompositionsResponse | jq '.rows[]? | {compositionId: .[0], ehr_id: .[1], template: .[2]}')
logInfo "$codexIdentifier" "$logEntry"
echo "end process pseudonym $codexIdentifier"
done;
## GET ehr id for given pseudonym 
#{
 # "q": "Select e/ehr_id/value from EHR e WHERE e/ehr_status/subject/external_ref/id/value = 'codex_6348Q7_test'"    
#}
## DELETE EHR by id http://localhost:8080/ehrbase/rest/admin/ehr/b140a403-90fb-46d7-8362-b1b0c242945f
