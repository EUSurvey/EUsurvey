package com.ec.survey.controller;

import com.ec.survey.model.AnswerSet;
import com.ec.survey.model.Archive;
import com.ec.survey.model.Draft;
import com.ec.survey.model.Export;
import com.ec.survey.model.Form;
import com.ec.survey.model.WebserviceTask;
import com.ec.survey.model.administration.User;
import com.ec.survey.model.survey.Survey;
import com.ec.survey.service.*;
import com.ec.survey.tools.AnswerExecutor;
import com.ec.survey.tools.ArchiveExecutor;
import com.ec.survey.tools.Constants;
import com.ec.survey.tools.RestoreExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/worker")
public class WorkerController extends BasicController {
		
	@Resource(name = "webserviceService")
	private WebserviceService webserviceService;

	@Resource(name = "taskExecutor")
	private TaskExecutor taskExecutor;
	
	private @Value("${smtpserver}") String smtpServer;
	private @Value("${smtp.port}") String smtpPort;
	private @Value("${webservice.maxrequestsperday}") String maxrequestsperday;		

	@RequestMapping(value = "createanswerpdf/{code}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String createanswerpdf(@PathVariable String code, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			AnswerSet answerSet = answerService.get(code);
			String email = request.getParameter(Constants.EMAIL);
			if (answerSet != null) {
				AnswerExecutor export = (AnswerExecutor) context.getBean("answerExecutor");
								
				if (email != null)
				{
					export.init(answerSet, email, sender, serverPrefix, false);
				} else {
					export.init( answerService.get(code));
				}
		
				taskExecutor.execute(export);
				return "OK";
			} else {
				return "INVALID DATA";
			}
						
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
	}
	
	@RequestMapping(value = "createdraftanswerpdf/{code}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String createdraftanswerpdf(@PathVariable String code, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			Draft draft = answerService.getDraftByAnswerUID(code);
			String email = request.getParameter(Constants.EMAIL);
			if (draft != null) {
				AnswerExecutor export = (AnswerExecutor) context.getBean("answerExecutor");
								
				if (email != null)
				{
					export.init(draft.getAnswerSet(), email, sender, serverPrefix, request.getSession().getAttribute("passwordauthentication") != null);
				} else {
					export.init(draft.getAnswerSet());
				}
		
				taskExecutor.execute(export);
				return "OK";
			} else {
				return "INVALID DATA";
			}
						
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
	}	
			
	@RequestMapping(value = "/start/{exportid}/{uid}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String start(@PathVariable String exportid, @PathVariable String uid, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			int id = Integer.parseInt(exportid);
			
			Export export = exportService.get(id, true);
			if (export == null)
			{
				return "export with that id not found";
			}
			
			Form form = new Form();
			form.setSurvey(export.getSurvey());
			
			exportService.prepareExport(form, export);
			exportService.startExport(form, export, false, resources, new Locale("en"), uid, null, true);
						
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}	
	
	@RequestMapping(value = "/startStatistics/{reqid}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String startStatistics(@PathVariable String reqid, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			int statisticsRequestId = Integer.parseInt(reqid);
			this.answerService.getStatisticsOrStartCreator(statisticsRequestId);
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}
		
	@RequestMapping(value = "/startRestore/{archiveid}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String startRestore(@PathVariable String archiveid, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			int id = Integer.parseInt(archiveid);
			
			Archive archive = archiveService.get(id);
			if (archive == null)
			{
				return "archive with that id not found";
			}
			
			User u = administrationService.getUser(archive.getUserId());			
			RestoreExecutor restore = (RestoreExecutor) context.getBean("restoreExecutor"); 
			restore.init(archive, archive.getSurveyShortname(), u);
			restore.prepare();
			taskExecutorLongRestore.execute(restore);			
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/deleteExportZombieFiles", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String deleteExportZombieFiles(HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			exportService.deleteExportZombieFiles();	
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/deleteOldExports", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String deleteOldExports(HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			exportService.deleteOldExports();	
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/deleteOldWebserviceExports", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String deleteOldWebserviceExports(HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			exportService.deleteOldWebserviceExports();	
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/startwebservice/{taskid}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String startwebservice(@PathVariable String taskid, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			int id = Integer.parseInt(taskid);
			
			WebserviceTask task = webserviceService.get(id);
			if (task == null)
			{
				return "task with that id not found";
			}
			
			webserviceService.startTask(task, new Locale("en"));
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}	
	
	@RequestMapping(value = "/restartwebservice/{taskid}", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "text/html")
	public @ResponseBody String restartwebservice(@PathVariable String taskid, HttpServletRequest request, HttpServletResponse response) {	
		
		try {
			int id = Integer.parseInt(taskid);
			
			WebserviceTask task = webserviceService.get(id);
			if (task == null)
			{
				return "task with that id not found";
			}
			
			webserviceService.restartTask(task);
		} catch (Exception e)
		{
			logger.error(e.getLocalizedMessage(), e);
			return e.getLocalizedMessage();
		}
		
		return "OK";
	}	
}
