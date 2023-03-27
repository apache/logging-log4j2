<#--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<#if entriesByType?size gt 0>== Changes
<#list entriesByType as entryType, entries>

=== ${entryType?capitalize}

<#list entries as entry>
* ${entry.description.text?replace("\\s+", " ", "r")} (for <@compress single_line=true>
<#list entry.issues as issue>${issue.link}[${issue.id}]<#if issue?has_next>, </#if></#list> by
<#list entry.authors as author>
<@compress single_line=true>
<#if author.name?has_content>${author.name}
<#elseif author.id == "rgoers">Ralph Goers
<#elseif author.id == "ggregory">Gary Gregory
<#elseif author.id == "sdeboy">Scott Deboy
<#elseif author.id == "rpopma">Remko Popma
<#elseif author.id == "nickwilliams">Nick Williams
<#elseif author.id == "mattsicker">Matt Sicker
<#elseif author.id == "bbrouwer">Bruce Brouwer
<#elseif author.id == "rgupta">Raman Gupta
<#elseif author.id == "mikes">Mikael Ståldal
<#elseif author.id == "ckozak">Carter Kozak
<#elseif author.id == "vy">Volkan Yazıcı
<#elseif author.id == "rgrabowski">Ron Grabowski
<#elseif author.id == "pkarwasz">Piotr P. Karwasz
<#else>`${author.id}`
</#if>
</@compress><#if author?has_next>, </#if>
</#list>
</@compress>)
</#list>
</#list>
</#if>
