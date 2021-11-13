

webapp.Dispatcher.service(){//��д��HttpServlet.service()����
    Router.Dest dest = router.resolve(method, pathInfo);
    Controller controller = injector.getInstance(dest.controllerClass);{
        return new HsController(){ //���䷽ʽ����HSController��ʵ��;
            super(app, conf, ctx, "History");{//new AppController()
                super(ctx);
                this.app = app;
                set(APP_ID, app.context.getApplicationID().toString());//����AppId;
                //����rm.web
                set(RM_WEB, JOINER.join(MRWebAppUtil.getYARNWebappScheme(),
                        WebAppUtils.getResolvedRemoteRMWebAppURLWithoutScheme(conf,MRWebAppUtil.getYARNHttpPolicy())));
            }
        }
    }
    
    dest.action.invoke(controller, (Object[]) null);{//HsController.job() -> AppController.job()
        requireJob();
        render(jobPage());
    }
}