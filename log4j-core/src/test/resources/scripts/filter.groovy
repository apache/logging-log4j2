return logEvent.marker?.isInstanceOf('FLOW') || logEvent.contextMap.containsKey('UserId')
