var result = false;
if (logEvent.getMarker() != null && logEvent.getMarker().isInstanceOf("FLOW")) {
	result = true;
} else if (logEvent.getContextMap().containsKey("UserId")) {
	result = true;
}
result;
