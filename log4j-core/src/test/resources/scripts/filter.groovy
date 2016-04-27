if (logEvent.getMarker() != null && logEvent.getMarker().isInstanceOf("FLOW")) {
    return true;
} else if (logEvent.getContextMap().containsKey("UserId")) {
    return true;
}
return false;
