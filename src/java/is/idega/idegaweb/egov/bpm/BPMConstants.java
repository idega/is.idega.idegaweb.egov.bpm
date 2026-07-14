package is.idega.idegaweb.egov.bpm;

public interface BPMConstants {

	public static final String	IW_BUNDLE_IDENTIFIER = "is.idega.idegaweb.egov.bpm",

								PLACEHOLDER_PROJECT_NAME = "#{projectName}",
								PLACEHOLDER_PERSONAL_ID = "#{personalId}",
								PLACEHOLDER_OWNER_NAME = "#{name}",
								PLACEHOLDER_GRANTED_AMOUNT = "#{projectCost}",
								PLACEHOLDER_OWNER_ADDRESS = "#{address}",
								PLACEHOLDER_OWNER_POSTAL_CODE = "#{postalCode}",
								PLACEHOLDER_OWNER_MUNICIPALITY = "#{municipality}",
								PLACEHOLDER_ANSWER_TO_OWNER = "#{ownerAnswer}",
								PLACEHOLDER_EXPENSES_WORK_COMPONENT = "#{expensesWorkComponent}",
								PLACEHOLDER_EXPENSES_ESTIMATED_COST = "#{expensesEstimatedCost}",
								PLACEHOLDER_EXPENSES_EXPECTED_GRANT = "#{expensesExpectedGrant}",
								PLACEHOLDER_EXPENSES_PROPOSAL_FOR_A_GRANT = "#{expensesProposalForAGrant}",

								ALL = "all",
								ASSIGNED_TO_ME = "assigned_to_me",
								ASSIGNED_TO_OTHERS = "assigned_to_others",

								FOR = "#for#",
								VARIABLE_NUMBER_OF_MISSED_PAYMENTS = "numberOfMissedPayments",
								VARIABLE_PROCESS_RULING_ATTACHMENTS = "files_processRullingAttachments",

								VARIABLE_TICKET_DATE = "date_ticketDate",

								APP_PROPERTY_BPM_VERSION = "bpm.version",

								PDF_FROM_EMAIL_CONTENT_FILE_NAME = "PDF_FROM_EMAIL_CONTENT_FILE_NAME",

								BPM2_VARIABLE_NAME_EMAIL_SENDERPERSONAL_NAME = "string_emailSenderPersonalName",
								BPM2_VARIABLE_NAME_FROM_ADDRESS = "string_fromAddress",
								BPM2_VARIABLE_NAME_SUBJECT = "string_subject",
								BPM2_VARIABLE_NAME_EMAIL_TEXT = "string_emailText",
								BPM2_VARIABLE_NAME_EMAIL_MESSAGE_RECEIVED_DATE = "date_emailMessageReceivedDate",
								BPM2_VARIABLE_NAME_ADDITIONALLY_ADDED_FILES = "files_additionallyAddedFiles",

								APP_PROPERTY_ATTACH_DOCUMENTS_TASK_FORM_NAME = "ATTACH_DOCUMENTS_TASK_FORM_NAME",
								ATTACH_DOCUMENTS_DEFAULT_TASK_FORM_NAME = "attach_files_form",

								CASES_EXPORT_EXTRA_DATA = "cases_export_extra_data",

								APP_PROPERTY_FILTER_OUT_SMALL_IMAGE_FILES_WHILE_PARSING_MESSAGES = "filter_out_small_image_files_while_parsing_messages",
								APP_PROPERTY_FILTER_OUT_ALL_IMAGE_FILES_WHILE_PARSING_MESSAGES = "filter_out_all_image_files_while_parsing_messages",

								APP_PROPERTY_MIN_IMAGE_SIZE_IN_BYTES_WHILE_PARSING_MESSAGES = "min_image_size_in_bytes_while_parsing_messages";

}