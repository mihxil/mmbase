/*
 * Copyright (c) 2003, 2004 Rafael Steil
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * This file creation date: Mar 28, 2003 / 8:09:08 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.view.admin;


import net.jforum.dao.DataAccessDriver;
import net.jforum.dao.RankingDAO;
import net.jforum.entities.Ranking;
import net.jforum.repository.RankingRepository;
import net.jforum.util.preferences.TemplateKeys;

/**
 * @author Rafael Steil
 * @version $Id: RankingAction.java,v 1.1 2008-01-17 07:48:24 mguo Exp $
 */
public class RankingAction extends AdminCommand 
{
	// List
	public void list() throws Exception
	{
		this.context.put("ranks", DataAccessDriver.getInstance().newRankingDAO().selectAll());
		this.setTemplateName(TemplateKeys.RANKING_LIST);
	}
	
	// One more, one more
	public void insert() throws Exception
	{
		this.setTemplateName(TemplateKeys.RANKING_INSERT);
		this.context.put("action", "insertSave");
	}
	
	// Edit
	public void edit() throws Exception
	{
		this.context.put("rank", DataAccessDriver.getInstance().newRankingDAO().selectById(
				this.request.getIntParameter("ranking_id")));
		this.setTemplateName(TemplateKeys.RANKING_EDIT);
		this.context.put("action", "editSave");
	}
	
	//  Save information
	public void editSave() throws Exception
	{
		Ranking r = new Ranking();
		r.setTitle(this.request.getParameter("rank_title"));
		r.setMin(this.request.getIntParameter("rank_min"));
		r.setId(this.request.getIntParameter("rank_id"));
		
		// TODO: needs to add support to images 
		
		DataAccessDriver.getInstance().newRankingDAO().update(r);
		RankingRepository.loadRanks();	
		this.list();
	}
	
	// Delete
	public void delete() throws Exception
	{
		String ids[] = this.request.getParameterValues("rank_id");
		
		RankingDAO rm = DataAccessDriver.getInstance().newRankingDAO();
		
		if (ids != null) {
			for (int i = 0; i < ids.length; i++) {
				rm.delete(Integer.parseInt(ids[i]));
			}
		}
			
		this.list();
	}
	
	// A new one
	public void insertSave() throws Exception
	{
		Ranking r = new Ranking();
		r.setTitle(this.request.getParameter("rank_title"));
		r.setMin(this.request.getIntParameter("rank_min"));
		
		// TODO: need to add support to images
		DataAccessDriver.getInstance().newRankingDAO().addNew(r);
		this.list();
	}
}
