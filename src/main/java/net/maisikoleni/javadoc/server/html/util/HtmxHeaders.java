package net.maisikoleni.javadoc.server.html.util;

public final class HtmxHeaders {

	private HtmxHeaders() {
	}

	public static final class Request {

		/**
		 * indicates that the request is via an element using hx-boost
		 */
		public static final String BOOSTED = "HX-Boosted";
		/**
		 * the current URL of the browser
		 */
		public static final String CURRENT_URL = "HX-Current-URL";
		/**
		 * true if the request is for history restoration after a miss in the local
		 * history cache
		 */
		public static final String HISTORY_RESTORE_REQUEST = "HX-History-Restore-Request";
		/**
		 * the user response to an hx-prompt
		 */
		public static final String PROMPT = "HX-Prompt";
		/**
		 * always true
		 */
		public static final String REQUEST = "HX-Request";
		/**
		 * the id of the target element if it exists
		 */
		public static final String TARGET = "HX-Target";
		/**
		 * the name of the triggered element if it exists
		 */
		public static final String TRIGGER_NAME = "HX-Trigger-Name";
		/**
		 * the id of the triggered element if it exists
		 */
		public static final String TRIGGER = "HX-Trigger";

		private Request() {
		}
	}

	public static final class Response {

		/**
		 * pushes a new url into the history stack
		 */
		public static final String PUSH_URL = "HX-Push-Url";
		/**
		 * replaces the current URL in the location bar
		 */
		public static final String REPLACE_URL = "HX-Replace-Url";
		/**
		 * can be used to do a client-side redirect to a new location
		 */
		public static final String REDIRECT = "HX-Redirect";
		/**
		 * Allows you to do a client-side redirect that does not do a full page reload
		 */
		public static final String LOCATION = "HX-Location";
		/**
		 * if set to "true" the client side will do a a full refresh of the page
		 */
		public static final String REFRESH = "HX-Refresh";
		/**
		 * A CSS selector that updates the target of the content update to a different
		 * element on the page
		 */
		public static final String RETARGET = "HX-Retarget";
		/**
		 * Allows you to specify how the response will be swapped. See hx-swap for
		 * possible values
		 */
		public static final String RESWAP = "HX-Reswap";
		/**
		 * allows you to trigger client side events, see the documentation for more info
		 */
		public static final String TRIGGER = "HX-Trigger";
		/**
		 * allows you to trigger client side events, see the documentation for more info
		 */
		public static final String TRIGGER_AFTER_SETTLE = "HX-Trigger-After-Settle";
		/**
		 * allows you to trigger client side events, see the documentation for more info
		 */
		public static final String TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap";

		private Response() {
		}
	}

}
