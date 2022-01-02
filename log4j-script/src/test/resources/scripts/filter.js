var result = false;
if (logEvent.getMarker() != null && logEvent.getMarker().isInstanceOf("FLOW")) {
	result = true;
} else if (logEvent.getContextData().containsKey("UserId")) {
	result = true;
}
result;
