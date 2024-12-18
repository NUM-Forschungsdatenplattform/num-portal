package org.highmed.numportal.domain.templates;

import org.highmed.numportal.service.exception.dto.ExceptionDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExceptionsTemplate {

  public static final String TOKEN_IS_NOT_VALID_MSG = "Token is not valid";
  public static final String RECORD_NOT_FOUND_MSG = "Record not found";
  public static final String RECORD_ALREADY_EXISTS = "Record already exists";
  public static final String PASS_NOT_MATCHING = "Password not match";

  public static final String USER_UNAUTHORISED_EXCEPTION = "User %s is unauthorized to access this profile!";
  public static final String USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE = "Username not found or no longer active";

  //ForbiddenException - AqlService
  public static final String CANNOT_ACCESS_THIS_AQL = "Cannot access this aql.";
  public static final String CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED = "Cannot access this resource. Logged in user is not approved.";
  public static final String AQL_EDIT_FOR_AQL_WITH_ID_IS_NOT_ALLOWED_AQL_HAS_DIFFERENT_OWNER = "Aql edit for aql with id: %s is not allowed. Aql has different owner.";
  public static final String CANNOT_DELETE_AQL = "Cannot delete aql: %s";
  public static final String CHANGING_COHORT_ONLY_ALLOWED_BY_THE_OWNER_OF_THE_PROJECT = "Changing cohort only allowed by the owner of the project";
  public static final String COHORT_CHANGE_ONLY_ALLOWED_ON_PROJECT_STATUS_DRAFT_OR_PENDING = "Cohort change only allowed on project status draft or pending";
  //CommentService
  public static final String COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR = "Comment edit for comment with id: %s not allowed. Comment has different author";
  public static final String CANNOT_DELETE_COMMENT = "Cannot delete comment: %s";

  //ResourceNotFound
  public static final String AQL_NOT_FOUND = "Aql not found: %s";
  public static final String CANNOT_FIND_AQL = "Cannot find aql: %s";
  public static final String CATEGORY_BY_ID_NOT_FOUND = "Category by id %s Not found";
  public static final String CATEGORY_WITH_ID_DOES_NOT_EXIST = "Category with id %s does not exist.";

  //BadRequestException - Organization Service
  public static final String CATEGORY_ID_CANT_BE_NULL = "Category id can't be null";
  public static final String THE_CATEGORY_IS_NOT_EMPTY_CANT_DELETE_IT = "The category is not empty, can't delete it.";
  public static final String COULD_NOT_SERIALIZE_AQL_VALIDATION_RESPONSE = "Could not serialize aql validation response: %s";
  public static final String INVALID_AQL_ID = "Invalid aql id";
  public static final String COHORT_NOT_FOUND = "Cohort not found: %s";
  public static final String COHORT_GROUP_CANNOT_BE_EMPTY = "Cohort group cannot be empty";
  public static final String INVALID_COHORT_GROUP_AQL_MISSING = "Invalid cohort group. Aql missing.";
  public static final String INVALID_COHORT_GROUP_CHILDREN_MISSING = "The query is invalid. Please select at least one criterion.";
  public static final String INVALID_COHORT_GROUP_AQL_MISSING_PARAMETERS = "The query is invalid. The value of at least one criterion is missing.";
  public static final String INVALID_COMMENT_ID = "Invalid commentId id";
  public static final String ORGANIZATION_NAME_MUST_BE_UNIQUE = "Organization name must be unique: %s";
  public static final String ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS = "Organization mail domain already exists: %s";
  public static final String ORGANIZATION_MAIL_DOMAIN_CANNOT_BE_NULL_OR_EMPTY = "Organization mail domain cannot be null or empty";
  public static final String INVALID_MAIL_DOMAIN = "Invalid mail domain: %s";
  public static final String ORGANIZATION_NOT_FOUND = "Organization not found: %s";

  public static final String ORGANIZATION_IS_NOT_EMPTY_CANT_DELETE_IT = "The organization :%s is not empty, can't delete it.";
  public static final String NOT_ALLOWED_TO_UPDATE_OWN_ORGANIZATION_STATUS = "Not allowed to update own's organization status";
  public static final String ATTACHMENT_NOT_FOUND = "Attachment with id %s not found";
  //ForbiddenException
  public static final String CANNOT_UPDATE_ORGANIZATION = "Cannot update organization: %s";
  public static final String CANNOT_ACCESS_THIS_RESOURCE = "Cannot access this resource";
  public static final String CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION = "Cannot assign user to deactivated organization: %s";

  //Project service
  public static final String RESEARCHER_NOT_FOUND = "Researcher not found.";
  public static final String RESEARCHER_NOT_APPROVED = "Researcher not approved.";
  public static final String INVALID_PROJECT_STATUS = "Invalid project status";
  public static final String INVALID_PROJECT_STATUS_PARAM = "Invalid project status: %s";
  public static final String PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED = "Project status transition from %s to %s is not allowed";
  public static final String PROJECT_COHORT_CANNOT_BE_NULL = "Project: %s cohort cannot be null";
  public static final String PROJECT_TEMPLATES_CANNOT_BE_NULL = "Project: %s templates cannot be null";
  public static final String PROJECT_NOT_FOUND = "Project not found: %s";
  public static final String MESSAGE_NOT_FOUND = "Message not found: %s";
  public static final String CAN_T_FIND_THE_COHORT_BY_ID = "Can't find the cohort by id: %s";
  public static final String AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL = "An issue has occurred, cannot execute aql";
  public static final String ERROR_WHILE_RETRIEVING_DATA = "Error while retrieving data: %s";
  public static final String ERROR_CREATING_A_ZIP_FILE_FOR_DATA_EXPORT = "Error creating a zip file for data export: %s";
  public static final String ERROR_WHILE_CREATING_THE_CSV_FILE = "Error while creating the CSV file: %s";
  public static final String MORE_THAN_ONE_TRANSITION_FROM_PUBLISHED_TO_CLOSED_FOR_PROJECT = "More than one transition from PUBLISHED to CLOSED for project: %s";
  public static final String ERROR_CREATING_THE_PROJECT_PDF = "Error creating the project PDF: %s";

  //CommentService
  public static final String PROJECT_DOES_NOT_EXIST = "Project does not exist";
  public static final String COMMENT_NOT_FOUND = "Comment not found: %s";

  //ForbiddenException
  public static final String CANNOT_ARCHIVE_PROJECT = "Cannot archive project: %s";
  public static final String CANNOT_DELETE_PROJECT = "Cannot delete project: %s";
  public static final String CANNOT_DELETE_PROJECT_INVALID_STATUS = "Cannot delete project: %s, invalid status: %s";
  public static final String CANNOT_UPDATE_PROJECT_INVALID_PROJECT_STATUS = "Cannot update project: %s, invalid project status: %s";
  public static final String CANNOT_UPDATE_MESSAGE_INVALID = "Cannot update message: %s, invalid: %s";
  public static final String CANNOT_HANDLE_DATE = "Cannot handle date: %s";
  public static final String CANNOT_DELETE_MESSAGE = "Cannot delete this message: %s";
  public static final String NO_PERMISSIONS_TO_EDIT_THIS_PROJECT = "No permissions to edit this project";
  public static final String CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_OWNER = "Cannot access this resource. User is not owner.";
  public static final String DATA_EXPLORER_AVAILABLE_FOR_PUBLISHED_PROJECTS_ONLY = "Data explorer available for published projects only";
  public static final String CANNOT_ACCESS_THIS_PROJECT = "Cannot access this project";

  public static final String NO_PERMISSIONS_TO_DELETE_ATTACHMENTS = "No permissions to delete attachments";

  public static final String CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS = "Not allowed to delete attachments , invalid project status: %s";

  public static final String CANNOT_DELETE_ATTACHMENT_INVALID_REVIEW_STATUS_COUNTER = "Not allowed to delete attachment %s because was already reviewed";


  //Template service
  public static final String CANNOT_FIND_TEMPLATE = "Cannot find template: %s";
  public static final String CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID = "Cannot create query for template with id: %s";

  //User service
  public static final String CANNOT_DELETE_APPROVED_USER = "Cannot delete user: %s; user is approved";
  public static final String UNKNOWN_ROLE = "Unknown Role";
  public static final String CANNOT_DELETE_ENABLED_USER = "Cannot delete user. User is enabled and email address is verified";
  public static final String USER_NOT_FOUND = "User not found: %s";
  public static final String ROLE_OR_USER_NOT_FOUND = "Role or user not found";
  public static final String NO_ROLES_FOUND = "No roles found";
  public static final String AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER = "An error has occurred, cannot retrieve users, please try again later: %s";
  public static final String AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER = "An error has occurred, please try again later: %s";
  public static final String AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER = "An error has occurred, cannot retrieve user roles, please try again later: %s";
  public static final String AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER = "An error has occurred while deleting user. Please try again later: %s";
  public static final String FETCHING_USER_FROM_KEYCLOAK_FAILED = "Fetching user from Keycloak failed";
  //ForbiddenException
  public static final String ORGANIZATION_ADMIN_CAN_ONLY_MANAGE_USERS_IN_THEIR_OWN_ORGANIZATION = "Organization admin can only manage users in their own organization.";
  public static final String NOT_ALLOWED_TO_REMOVE_THAT_ROLE = "Not allowed to remove that role";
  public static final String NOT_ALLOWED_TO_SET_THAT_ROLE = "Not allowed to set that role";
  public static final String CAN_ONLY_CHANGE_OWN_NAME_ORG_ADMIN_NAMES_OF_THE_PEOPLE_IN_THE_ORGANIZATION_AND_SUPERUSER_ALL_NAMES = "Can only change own name, org admin names of the people in the organization and superuser all names.";
  public static final String NOT_ALLOWED_TO_UPDATE_OWN_STATUS = "Not allowed to update own status";

  //EhrBaseService service
  public static final String NO_DATA_COLUMNS_IN_THE_QUERY_RESULT = "No data columns in the query result";
  public static final String AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL = "An error has occurred, cannot execute aql: %s";
  public static final String AN_ERROR_HAS_OCCURRED_CANNOT_GET_TEMPLATES = "An error has occurred, cannot retrieve templates: %s";
  public static final String QUERY_RESULT_DOESN_T_CONTAIN_EHR_STATUS_COLUMN = "query result doesn't contain ehr_status column";

  //PrivacyException
  public static final String TOO_FEW_MATCHES_RESULTS_WITHHELD_FOR_PRIVACY_REASONS = "Too few matches, results withheld for privacy reasons.";
  public static final String RESULTS_WITHHELD_FOR_PRIVACY_REASONS = "Number of matches below threshold, results withheld for privacy reasons.";

  //Pseudonymity
  public static final String EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND = "Ehr Id matching the pseudonym was not found";
  public static final String PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED = "Pseudonymity secret is not configured";

  //ContentService
  public static final String COULDN_T_PARSE_NAVIGATION_CONTENT = "Couldn't parse navigation content: %s";
  public static final String COULDN_T_SAVE_NAVIGATION_CONTENT = "Couldn't save navigation content: %s";
  public static final String COULDN_T_PARSE_CARD = "Couldn't parse card: %s";
  public static final String COULDN_T_SAVE_CARD = "Couldn't save card: %s";

  //CompositionFlattener
  public static final String CANNOT_PARSE_RESULTS = "Cannot parse results: %s";
  public static final String CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID = "Cannot parse results, composition missing template id";

  //Policy
  public static final String INVALID_AQL = "Invalid aql";
  public static final String COHORT_SIZE_CANNOT_BE_0 = "Cohort size cannot be 0";
  public static final String NO_TEMPLATES_ATTACHED_TO_THE_PROJECT = "No templates attached to the project";

  //IllegalArgumentException
  public static final String CANNOT_EXECUTE_AN_EMPTY_COHORT = "Cannot execute an empty cohort";
  public static final String RELATIVE_COMPLEMENT_REQUIRES_TWO_VALID_SETS = "Relative complement requires two valid sets";

  public static final String INVALID_AQL_QUERY = "EhrBase - Malformed query exception: {}";
  public static final String ERROR_MESSAGE = "EhrBase - An error has occurred while calling EhrBase: {} ";
  public static final String CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED = "Cannot check consent for data usage outside the European Union, oid not configured";
  public static final String CACHE_IS_NOT_REACHABLE = "Cache is not reachable";
  public static final String EXCEPTION_HAPPENED_IN_CLASS_FOR_ENVIRONMENT = "Exception happened in %s class for %s environment. Link %s is not accessible"; //3 parameters

  //document
  public static final String DOCUMENT_TYPE_MISMATCH = "Document type mismatch. Only PDF type is allowed to be uploaded.";
  public static final String INVALID_FILE_MISSING_CONTENT = "Invalid file. Missing content";
  public static final String PDF_FILE_SIZE_EXCEEDED = "PDF File Size Exceeded. Maximum file size is [%s] MB. Current file size is [%s]+ MB.";
  public static final String PDF_FILES_ARE_NOT_ATTACHED = "PDF Files are not attached to the project.";
  public static final String ATTACHMENT_LIMIT_REACHED = "Attachment limit reached. Maximum of 10 attachments can be assigned to a project.";
  public static final String WRONG_PROJECT_STATUS = "Wrong project status [%s]. Only projects with status 'Draft' or 'Change Request' can accept attachments.";
  public static final String DESCRIPTION_TOO_LONG = "Description is too long. Only 255 characters are accepted for description. [%s]";

  public static final String CLAMAV_PING_FAILED = "Could not ping ClamAV service";
  public static final String CLAMAV_SCAN_FAILED = "Could not scan file %s";

  public static final Map<String, ExceptionDto> errorMap = new HashMap<>();

  static {
    errorMap.put(TOKEN_IS_NOT_VALID_MSG, new ExceptionDto(1, new ArrayList<>()));
    errorMap.put(RECORD_NOT_FOUND_MSG, new ExceptionDto(2, new ArrayList<>()));

    errorMap.put(RECORD_ALREADY_EXISTS, new ExceptionDto(3, new ArrayList<>()));
    errorMap.put(PASS_NOT_MATCHING, new ExceptionDto(4, new ArrayList<>()));

    errorMap.put(USER_UNAUTHORISED_EXCEPTION, new ExceptionDto(5, new ArrayList<>())); //1 parameter
    errorMap.put(USERNAME_NOT_FOUND_OR_NO_LONGER_ACTIVE, new ExceptionDto(6, new ArrayList<>()));

    //Organization Service
    errorMap.put(CATEGORY_ID_CANT_BE_NULL, new ExceptionDto(7, new ArrayList<>()));
    errorMap.put(THE_CATEGORY_IS_NOT_EMPTY_CANT_DELETE_IT, new ExceptionDto(8, new ArrayList<>()));
    errorMap.put(COULD_NOT_SERIALIZE_AQL_VALIDATION_RESPONSE, new ExceptionDto(9, new ArrayList<>()));
    errorMap.put(INVALID_AQL_ID, new ExceptionDto(10, new ArrayList<>()));
    errorMap.put(COHORT_NOT_FOUND, new ExceptionDto(11, new ArrayList<>())); //1 parameter
    errorMap.put(COHORT_GROUP_CANNOT_BE_EMPTY, new ExceptionDto(12, new ArrayList<>()));
    errorMap.put(INVALID_COHORT_GROUP_AQL_MISSING, new ExceptionDto(13, new ArrayList<>()));
    errorMap.put(INVALID_COMMENT_ID, new ExceptionDto(14, new ArrayList<>()));
    errorMap.put(ORGANIZATION_NAME_MUST_BE_UNIQUE, new ExceptionDto(15, new ArrayList<>())); //1 parameter
    errorMap.put(ORGANIZATION_MAIL_DOMAIN_ALREADY_EXISTS, new ExceptionDto(16, new ArrayList<>())); //1 parameter
    errorMap.put(ORGANIZATION_MAIL_DOMAIN_CANNOT_BE_NULL_OR_EMPTY, new ExceptionDto(17, new ArrayList<>()));
    errorMap.put(INVALID_MAIL_DOMAIN, new ExceptionDto(18, new ArrayList<>())); //1 parameter

    //Project service
    errorMap.put(RESEARCHER_NOT_FOUND, new ExceptionDto(19, new ArrayList<>()));
    errorMap.put(RESEARCHER_NOT_APPROVED, new ExceptionDto(20, new ArrayList<>()));
    errorMap.put(INVALID_PROJECT_STATUS, new ExceptionDto(21, new ArrayList<>()));
    errorMap.put(INVALID_PROJECT_STATUS_PARAM, new ExceptionDto(22, new ArrayList<>())); //1 parameter
    errorMap.put(PROJECT_STATUS_TRANSITION_FROM_TO_IS_NOT_ALLOWED, new ExceptionDto(23, new ArrayList<>())); //2 parameters
    errorMap.put(PROJECT_COHORT_CANNOT_BE_NULL, new ExceptionDto(24, new ArrayList<>())); //1 parameter
    errorMap.put(PROJECT_TEMPLATES_CANNOT_BE_NULL, new ExceptionDto(25, new ArrayList<>())); //1 parameter
    errorMap.put(PROJECT_NOT_FOUND, new ExceptionDto(26, new ArrayList<>())); //1 parameter

    //Template service
    errorMap.put(CANNOT_FIND_TEMPLATE, new ExceptionDto(27, new ArrayList<>())); //1 parameter

    //User service
    errorMap.put(CANNOT_DELETE_APPROVED_USER, new ExceptionDto(28, new ArrayList<>())); //1 parameter
    errorMap.put(UNKNOWN_ROLE, new ExceptionDto(29, new ArrayList<>()));
    errorMap.put(CANNOT_DELETE_ENABLED_USER, new ExceptionDto(30, new ArrayList<>()));

    //EhrBaseService service
    errorMap.put(NO_DATA_COLUMNS_IN_THE_QUERY_RESULT, new ExceptionDto(31, new ArrayList<>()));

    //ForbiddenException - AqlService
    errorMap.put(CANNOT_ACCESS_THIS_AQL, new ExceptionDto(32, new ArrayList<>()));
    errorMap.put(CANNOT_ARCHIVE_PROJECT, new ExceptionDto(33, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_APPROVED, new ExceptionDto(34, new ArrayList<>()));
    errorMap.put(AQL_EDIT_FOR_AQL_WITH_ID_IS_NOT_ALLOWED_AQL_HAS_DIFFERENT_OWNER, new ExceptionDto(35, new ArrayList<>()));
    errorMap.put(CANNOT_DELETE_AQL, new ExceptionDto(36, new ArrayList<>()));
    errorMap.put(CHANGING_COHORT_ONLY_ALLOWED_BY_THE_OWNER_OF_THE_PROJECT, new ExceptionDto(37, new ArrayList<>()));
    errorMap.put(COHORT_CHANGE_ONLY_ALLOWED_ON_PROJECT_STATUS_DRAFT_OR_PENDING, new ExceptionDto(38, new ArrayList<>()));
    errorMap.put(COMMENT_EDIT_FOR_COMMENT_WITH_ID_IS_NOT_ALLOWED_COMMENT_HAS_DIFFERENT_AUTHOR, new ExceptionDto(39, new ArrayList<>()));
    errorMap.put(CANNOT_DELETE_COMMENT, new ExceptionDto(40, new ArrayList<>())); //1 parameter
    //organization
    errorMap.put(CANNOT_UPDATE_ORGANIZATION, new ExceptionDto(41, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_ACCESS_THIS_RESOURCE, new ExceptionDto(42, new ArrayList<>()));
    //project
    errorMap.put(CANNOT_DELETE_PROJECT, new ExceptionDto(43, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_DELETE_PROJECT_INVALID_STATUS, new ExceptionDto(44, new ArrayList<>())); //2 parameters
    errorMap.put(CANNOT_UPDATE_PROJECT_INVALID_PROJECT_STATUS, new ExceptionDto(45, new ArrayList<>())); //2 parameters
    errorMap.put(NO_PERMISSIONS_TO_EDIT_THIS_PROJECT, new ExceptionDto(46, new ArrayList<>()));
    errorMap.put(CANNOT_ACCESS_THIS_RESOURCE_USER_IS_NOT_OWNER, new ExceptionDto(47, new ArrayList<>()));
    errorMap.put(DATA_EXPLORER_AVAILABLE_FOR_PUBLISHED_PROJECTS_ONLY, new ExceptionDto(48, new ArrayList<>()));
    errorMap.put(CANNOT_ACCESS_THIS_PROJECT, new ExceptionDto(49, new ArrayList<>()));
    //user-service
    errorMap.put(ORGANIZATION_ADMIN_CAN_ONLY_MANAGE_USERS_IN_THEIR_OWN_ORGANIZATION, new ExceptionDto(50, new ArrayList<>()));
    errorMap.put(NOT_ALLOWED_TO_REMOVE_THAT_ROLE, new ExceptionDto(51, new ArrayList<>()));
    errorMap.put(NOT_ALLOWED_TO_SET_THAT_ROLE, new ExceptionDto(52, new ArrayList<>()));
    errorMap.put(
        CAN_ONLY_CHANGE_OWN_NAME_ORG_ADMIN_NAMES_OF_THE_PEOPLE_IN_THE_ORGANIZATION_AND_SUPERUSER_ALL_NAMES, new ExceptionDto(53, new ArrayList<>()));

    //PrivacyException
    errorMap.put(TOO_FEW_MATCHES_RESULTS_WITHHELD_FOR_PRIVACY_REASONS, new ExceptionDto(54, new ArrayList<>()));

    //ResourceNotFound
    errorMap.put(AQL_NOT_FOUND, new ExceptionDto(55, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_FIND_AQL, new ExceptionDto(56, new ArrayList<>())); //1 parameter
    errorMap.put(CATEGORY_BY_ID_NOT_FOUND, new ExceptionDto(57, new ArrayList<>())); //1 parameter
    errorMap.put(CATEGORY_WITH_ID_DOES_NOT_EXIST, new ExceptionDto(58, new ArrayList<>())); //1 parameter

    //CommentService
    errorMap.put(PROJECT_DOES_NOT_EXIST, new ExceptionDto(59, new ArrayList<>()));
    errorMap.put(COMMENT_NOT_FOUND, new ExceptionDto(60, new ArrayList<>()));
    //OrganizationService
    errorMap.put(ORGANIZATION_NOT_FOUND, new ExceptionDto(61, new ArrayList<>())); //1 parameter
    errorMap.put(ORGANIZATION_IS_NOT_EMPTY_CANT_DELETE_IT, new ExceptionDto(98, new ArrayList<>()));
    //UserDetailsService
    errorMap.put(USER_NOT_FOUND, new ExceptionDto(62, new ArrayList<>())); //1 parameter
    //UserService
    errorMap.put(ROLE_OR_USER_NOT_FOUND, new ExceptionDto(63, new ArrayList<>()));
    errorMap.put(NO_ROLES_FOUND, new ExceptionDto(64, new ArrayList<>()));
    //Pseudonymity
    errorMap.put(EHR_ID_MATCHING_THE_PSEUDONYM_WAS_NOT_FOUND, new ExceptionDto(65, new ArrayList<>()));

    //SystemException
    errorMap.put(COULDN_T_PARSE_NAVIGATION_CONTENT, new ExceptionDto(66, new ArrayList<>())); //1 parameter
    errorMap.put(COULDN_T_SAVE_NAVIGATION_CONTENT, new ExceptionDto(67, new ArrayList<>())); //1 parameter
    errorMap.put(COULDN_T_PARSE_CARD, new ExceptionDto(68, new ArrayList<>())); //1 parameter
    errorMap.put(COULDN_T_SAVE_CARD, new ExceptionDto(69, new ArrayList<>())); //1 parameter
    //CompositionFlattener
    errorMap.put(CANNOT_PARSE_RESULTS, new ExceptionDto(70, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID, new ExceptionDto(71, new ArrayList<>()));
    errorMap.put(AN_ERROR_HAS_OCCURRED_CANNOT_EXECUTE_AQL, new ExceptionDto(72, new ArrayList<>())); //1 parameter
    errorMap.put(AN_ERROR_HAS_OCCURRED_CANNOT_GET_TEMPLATES, new ExceptionDto(102, new ArrayList<>())); //1 parameter
    //CohortService
    errorMap.put(CAN_T_FIND_THE_COHORT_BY_ID, new ExceptionDto(73, new ArrayList<>())); //1 parameter
    //ProjectService
    errorMap.put(AN_ISSUE_HAS_OCCURRED_CANNOT_EXECUTE_AQL, new ExceptionDto(74, new ArrayList<>()));
    errorMap.put(ERROR_WHILE_RETRIEVING_DATA, new ExceptionDto(75, new ArrayList<>())); //1 parameter
    errorMap.put(ERROR_CREATING_A_ZIP_FILE_FOR_DATA_EXPORT, new ExceptionDto(76, new ArrayList<>())); //1 parameter
    errorMap.put(ERROR_WHILE_CREATING_THE_CSV_FILE, new ExceptionDto(77, new ArrayList<>())); //1 parameter
    errorMap.put(MORE_THAN_ONE_TRANSITION_FROM_PUBLISHED_TO_CLOSED_FOR_PROJECT, new ExceptionDto(78, new ArrayList<>())); //1 parameter
    errorMap.put(ERROR_CREATING_THE_PROJECT_PDF, new ExceptionDto(79, new ArrayList<>())); //1 parameter
    //TemplateService
    errorMap.put(CANNOT_CREATE_QUERY_FOR_TEMPLATE_WITH_ID, new ExceptionDto(80, new ArrayList<>())); //1 parameter
    //UserService
    errorMap.put(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USERS_PLEASE_TRY_AGAIN_LATER, new ExceptionDto(81, new ArrayList<>())); //1 parameter
    errorMap.put(AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER, new ExceptionDto(82, new ArrayList<>())); //1 parameter
    errorMap.put(AN_ERROR_HAS_OCCURRED_CANNOT_RETRIEVE_USER_ROLES_PLEASE_TRY_AGAIN_LATER, new ExceptionDto(83, new ArrayList<>())); //1 parameter
    errorMap.put(AN_ERROR_HAS_OCCURRED_WHILE_DELETING_USER_PLEASE_TRY_AGAIN_LATER, new ExceptionDto(84, new ArrayList<>())); //1 parameter
    errorMap.put(FETCHING_USER_FROM_KEYCLOAK_FAILED, new ExceptionDto(85, new ArrayList<>()));
    //EhrBaseService
    errorMap.put(QUERY_RESULT_DOESN_T_CONTAIN_EHR_STATUS_COLUMN, new ExceptionDto(86, new ArrayList<>()));
    //Pseudonymity
    errorMap.put(PSEUDONYMITY_SECRET_IS_NOT_CONFIGURED, new ExceptionDto(87, new ArrayList<>()));

    //Policy
    errorMap.put(INVALID_AQL, new ExceptionDto(88, new ArrayList<>()));
    //EhrPolicy
    errorMap.put(COHORT_SIZE_CANNOT_BE_0, new ExceptionDto(89, new ArrayList<>()));

    //IllegalArgumentException
    errorMap.put(CANNOT_EXECUTE_AN_EMPTY_COHORT, new ExceptionDto(90, new ArrayList<>()));
    errorMap.put(RELATIVE_COMPLEMENT_REQUIRES_TWO_VALID_SETS, new ExceptionDto(91, new ArrayList<>()));

    //PrivacyException
    errorMap.put(RESULTS_WITHHELD_FOR_PRIVACY_REASONS, new ExceptionDto(92, new ArrayList<>()));
    errorMap.put(INVALID_AQL_QUERY, new ExceptionDto(93, new ArrayList<>()));
    errorMap.put(CANNOT_CHECK_CONSENT_FOR_DATA_USAGE_OUTSIDE_THE_EUROPEAN_UNION_OID_NOT_CONFIGURED, new ExceptionDto(95, new ArrayList<>()));

    //Policy
    errorMap.put(NO_TEMPLATES_ATTACHED_TO_THE_PROJECT, new ExceptionDto(96, new ArrayList<>()));
    errorMap.put(NOT_ALLOWED_TO_UPDATE_OWN_STATUS, new ExceptionDto(97, new ArrayList<>()));
    errorMap.put(CACHE_IS_NOT_REACHABLE, new ExceptionDto(98, new ArrayList<>()));

    errorMap.put(NOT_ALLOWED_TO_UPDATE_OWN_ORGANIZATION_STATUS, new ExceptionDto(99, new ArrayList<>()));
    errorMap.put(CANNOT_ASSIGN_USER_TO_DEACTIVATED_ORGANIZATION, new ExceptionDto(100, new ArrayList<>())); //1 parameter
    errorMap.put(EXCEPTION_HAPPENED_IN_CLASS_FOR_ENVIRONMENT, new ExceptionDto(101, new ArrayList<>())); //3 parameter
    errorMap.put(ATTACHMENT_NOT_FOUND, new ExceptionDto(104, new ArrayList<>())); //1 parameter

    //Document
    errorMap.put(DOCUMENT_TYPE_MISMATCH, new ExceptionDto(105, new ArrayList<>()));
    errorMap.put(INVALID_FILE_MISSING_CONTENT, new ExceptionDto(106, new ArrayList<>()));
    errorMap.put(PDF_FILE_SIZE_EXCEEDED, new ExceptionDto(107, new ArrayList<>())); //2 parameters

    errorMap.put(CLAMAV_PING_FAILED, new ExceptionDto(108, new ArrayList<>()));
    errorMap.put(CLAMAV_SCAN_FAILED, new ExceptionDto(109, new ArrayList<>())); //1 parameter
    errorMap.put(NO_PERMISSIONS_TO_DELETE_ATTACHMENTS, new ExceptionDto(110, new ArrayList<>()));
    errorMap.put(CANNOT_DELETE_ATTACHMENTS_INVALID_PROJECT_STATUS, new ExceptionDto(111, new ArrayList<>())); //1 parameter
    errorMap.put(CANNOT_DELETE_ATTACHMENT_INVALID_REVIEW_STATUS_COUNTER, new ExceptionDto(112, new ArrayList<>()));

    errorMap.put(PDF_FILES_ARE_NOT_ATTACHED, new ExceptionDto(110, new ArrayList<>()));
    errorMap.put(ATTACHMENT_LIMIT_REACHED, new ExceptionDto(111, new ArrayList<>()));
    errorMap.put(WRONG_PROJECT_STATUS, new ExceptionDto(112, new ArrayList<>()));
    errorMap.put(DESCRIPTION_TOO_LONG, new ExceptionDto(113, new ArrayList<>())); //1 parameter
  }

  private ExceptionsTemplate() {

  }
}
