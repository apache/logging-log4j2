package scripts

return logEvent.marker?.isInstanceOf('FLOW') || logEvent.contextData.containsKey('UserId')
