package io.async.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryResult
{
	final List<String> titles;
	final ErrorResponse error;

	public QueryResult(List<String> titles)
	{
		this.titles = titles;
		this.error = null;
	}

	public QueryResult(ErrorResponse error)
	{
		this.titles = null;
		this.error = error;
	}

	public List<String> getTitles()
	{
		return titles;
	}

	public ErrorResponse getError()
	{
		return error;
	}
}
