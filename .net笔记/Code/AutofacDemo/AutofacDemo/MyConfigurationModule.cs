using Autofac;
using Autofac.Core;
using Autofac.Core.Registration;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AutofacDemo
{
    public class MyConfigurationModule:Module
    {
        protected override void AttachToComponentRegistration(IComponentRegistryBuilder componentRegistry, IComponentRegistration registration)
        {
            base.AttachToComponentRegistration(componentRegistry, registration);
        }
        protected override void AttachToRegistrationSource(IComponentRegistryBuilder componentRegistry, IRegistrationSource registrationSource)
        {
            base.AttachToRegistrationSource(componentRegistry, registrationSource);
        }
        protected override void Load(ContainerBuilder builder)
        {
            builder.RegisterType<BLL.TestServiceAimpl>().As<IBLL.ITestServiceA>();
            builder.RegisterType<BLL.TestServiceBimpl>().As<IBLL.ITestServiceB>().PropertiesAutowired();//属性注入
        }
        protected override System.Reflection.Assembly ThisAssembly => base.ThisAssembly;
    }
}
