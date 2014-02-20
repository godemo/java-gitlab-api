package org.gitlab.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gitlab.api.http.GitlabHTTPRequestor;
import org.gitlab.api.models.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Gitlab API Wrapper class
 *
 * @author @timols
 */
public class GitlabAPI {
    private final String _hostUrl;
    private final String _apiToken;
    private boolean _ignoreCertificateErrors = false;
    private static final String API_NAMESPACE = "/api/v3";
    public static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private GitlabAPI(String hostUrl, String apiToken) {
        _hostUrl = hostUrl.endsWith("/") ? hostUrl.replaceAll("/$", "") : hostUrl;
        _apiToken = apiToken;
    }

    public static GitlabAPI connect(String hostUrl, String apiToken) {
        return new GitlabAPI(hostUrl, apiToken);
    }

    public GitlabAPI ignoreCertificateErrors(boolean ignoreCertificateErrors) {
        _ignoreCertificateErrors = ignoreCertificateErrors;
        return this;
    }

    public GitlabHTTPRequestor retrieve() {
        return new GitlabHTTPRequestor(this);
    }

    public GitlabHTTPRequestor dispatch() {
        return new GitlabHTTPRequestor(this).method("POST");
    }

    public boolean isIgnoreCertificateErrors() {
        return _ignoreCertificateErrors;
    }

    public URL getAPIUrl(String tailAPIUrl) throws IOException {
        if (_apiToken != null) {
            tailAPIUrl = tailAPIUrl + (tailAPIUrl.indexOf('?') > 0 ? '&' : '?') + "private_token=" + _apiToken;
        }

        if (!tailAPIUrl.startsWith("/")) {
            tailAPIUrl = "/" + tailAPIUrl;
        }
        return new URL(_hostUrl + API_NAMESPACE + tailAPIUrl);
    }

    public URL getUrl(String tailAPIUrl) throws IOException {
        if (!tailAPIUrl.startsWith("/")) {
            tailAPIUrl = "/" + tailAPIUrl;
        }

        return new URL(_hostUrl + tailAPIUrl);
    }

    public GitlabProject getProject(Integer projectId) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + projectId;
        return retrieve().to(tailUrl, GitlabProject.class);
    }

    public List<GitlabProject> getProjects() throws IOException {
        String tailUrl = GitlabProject.URL;
        return Arrays.asList(retrieve().to(tailUrl, GitlabProject[].class));
    }

    public List<GitlabProject> getAllProjects() throws IOException {
        String tailUrl = GitlabProject.URL;
        List<GitlabProject> results = new ArrayList<GitlabProject>();
        Iterator<GitlabProject[]> iterator = retrieve().asIterator(tailUrl, GitlabProject[].class);

        while (iterator.hasNext()) {
            GitlabProject[] projects = iterator.next();

            if (projects.length > 0) {
                results.addAll(Arrays.asList(projects));
            }
        }

        return results;

    }

    public List<GitlabMergeRequest> getOpenMergeRequests(GitlabProject project) throws IOException {
        List<GitlabMergeRequest> allMergeRequests = getAllMergeRequests(project);
        List<GitlabMergeRequest> openMergeRequests = new ArrayList<GitlabMergeRequest>();

        for (GitlabMergeRequest mergeRequest : allMergeRequests) {
            if (mergeRequest.isMerged() || mergeRequest.isClosed()) {
                continue;
            }
            openMergeRequests.add(mergeRequest);
        }

        return openMergeRequests;
    }

    public List<GitlabMergeRequest> getMergeRequests(Integer projectId) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + projectId + GitlabMergeRequest.URL;
        return fetchMergeRequests(tailUrl);
    }

    public List<GitlabMergeRequest> getMergeRequests(GitlabProject project) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabMergeRequest.URL;
        return fetchMergeRequests(tailUrl);
    }

    public List<GitlabMergeRequest> getAllMergeRequests(GitlabProject project) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabMergeRequest.URL;
        List<GitlabMergeRequest> results = new ArrayList<GitlabMergeRequest>();
        Iterator<GitlabMergeRequest[]> iterator = retrieve().asIterator(tailUrl, GitlabMergeRequest[].class);

        while (iterator.hasNext()) {
            GitlabMergeRequest[] requests = iterator.next();

            if (requests.length > 0) {
                results.addAll(Arrays.asList(requests));
            }
        }

        return results;
    }

    public GitlabMergeRequest getMergeRequest(GitlabProject project, Integer mergeRequestId) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + project.getId() + "/merge_request/" + mergeRequestId;
        return retrieve().to(tailUrl, GitlabMergeRequest.class);
    }

    public List<GitlabNote> getNotes(GitlabMergeRequest mergeRequest) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                GitlabMergeRequest.URL + "/" + mergeRequest.getId() +
                GitlabNote.URL;

        GitlabNote[] notes = retrieve().to(tailUrl, GitlabNote[].class);
        return Arrays.asList(notes);
    }

    public List<GitlabNote> getAllNotes(GitlabMergeRequest mergeRequest) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                GitlabMergeRequest.URL + "/" + mergeRequest.getId() +
                GitlabNote.URL;

        List<GitlabNote> results = new ArrayList<GitlabNote>();
        Iterator<GitlabNote[]> iterator = retrieve().asIterator(tailUrl, GitlabNote[].class);

        while (iterator.hasNext()) {
            GitlabNote[] projects = iterator.next();

            if (projects.length > 0) {
                results.addAll(Arrays.asList(projects));
            }
        }

        return results;

    }

    public List<GitlabCommit> getCommits(GitlabMergeRequest mergeRequest) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                "/repository" + GitlabCommit.URL + "?ref_name=" + mergeRequest.getSourceBranch();

        GitlabCommit[] commits = retrieve().to(tailUrl, GitlabCommit[].class);
        return Arrays.asList(commits);
    }

    public GitlabNote createNote(GitlabMergeRequest mergeRequest, String body) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                GitlabMergeRequest.URL + "/" + mergeRequest.getId() + GitlabNote.URL;

        return dispatch().with("body", body).to(tailUrl, GitlabNote.class);
    }
    
    public List<GitlabBranch> getBranches(GitlabProject project) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabBranch.URL;
        GitlabBranch[] branches = retrieve().to(tailUrl, GitlabBranch[].class);
        return Arrays.asList(branches);
    }
    
    public GitlabBranch getBranch(GitlabProject project, String branchName) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabBranch.URL + branchName;
        GitlabBranch branch = retrieve().to(tailUrl, GitlabBranch.class);
        return branch;
    }
    
    public void protectBranch(GitlabProject project, String branchName) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabBranch.URL + branchName + "/protect";
        retrieve().method("PUT").to(tailUrl, Void.class);
    }
    
    public void unprotectBranch(GitlabProject project, String branchName) throws IOException {
        String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabBranch.URL + branchName + "/unprotect";
        retrieve().method("PUT").to(tailUrl, Void.class);
    }
    
    public List<GitlabProjectHook> getProjectHooks(GitlabProject project) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabProjectHook.URL;
    	GitlabProjectHook[] hooks = retrieve().to(tailUrl, GitlabProjectHook[].class);
        return Arrays.asList(hooks);
    }
    
    public GitlabProjectHook getProjectHook(GitlabProject project, String hookId) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabProjectHook.URL + "/" + hookId;
    	GitlabProjectHook hook = retrieve().to(tailUrl, GitlabProjectHook.class);
        return hook;
    }
    
    public GitlabProjectHook addProjectHook(GitlabProject project, String url) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabProjectHook.URL + "?url=" + URLEncoder.encode(url, "UTF-8");
    	return dispatch().to(tailUrl, GitlabProjectHook.class);
    }
    
    public GitlabProjectHook editProjectHook(GitlabProject project, String hookId, String url) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabProjectHook.URL + "/" + hookId + "?url=" + URLEncoder.encode(url, "UTF-8");
    	return retrieve().method("PUT").to(tailUrl, GitlabProjectHook.class);
    }
    
    public void deleteProjectHook(GitlabProject project, String hookId) throws IOException {
    	String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabProjectHook.URL + "/" + hookId;
    	retrieve().method("DELETE").to(tailUrl, Void.class);
    }

    private List<GitlabMergeRequest> fetchMergeRequests(String tailUrl) throws IOException {
        GitlabMergeRequest[] mergeRequests = retrieve().to(tailUrl, GitlabMergeRequest[].class);
        return Arrays.asList(mergeRequests);
    }
}
