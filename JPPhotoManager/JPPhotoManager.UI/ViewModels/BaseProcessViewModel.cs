using JPPhotoManager.Application;
using JPPhotoManager.Domain;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using System.Windows;

namespace JPPhotoManager.UI.ViewModels
{
    public abstract class BaseProcessViewModel<C, R> : BaseViewModel
    {
        private ProcessStepEnum step = ProcessStepEnum.ViewDescription;
        private ObservableCollection<string> processStatusMessages;
        private ObservableCollection<R> results;

        protected BaseProcessViewModel(IApplication application) : base(application)
        {
            processStatusMessages = new ObservableCollection<string>();
        }

        public ObservableCollection<string> ProcessStatusMessages
        {
            get { return processStatusMessages; }
        }

        public ObservableCollection<R> Results
        {
            get { return results; }
            set
            {
                results = value;
                NotifyPropertyChanged(nameof(Results));
                NotifyPropertyChanged(nameof(CanViewResults));
            }
        }

        public abstract C GetProcessConfiguration();

        public abstract void SetProcessConfiguration(C configuration);

        public abstract string Description { get; }

        public ProcessStepEnum Step
        {
            get { return step; }

            private set
            {
                step = value;
                NotifyPropertyChanged(
                    nameof(Step),
                    nameof(DescriptionVisible),
                    nameof(ConfigurationVisible),
                    nameof(RunVisible),
                    nameof(ResultsVisible),
                    nameof(CanConfigure));
            }
        }

        public Visibility DescriptionVisible
        {
            get { return Step == ProcessStepEnum.ViewDescription ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ConfigurationVisible
        {
            get { return Step == ProcessStepEnum.Configure ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility RunVisible
        {
            get { return Step == ProcessStepEnum.Run ? Visibility.Visible : Visibility.Hidden; }
        }

        public Visibility ResultsVisible
        {
            get { return Step == ProcessStepEnum.ViewResults ? Visibility.Visible : Visibility.Hidden; }
        }

        public bool CanViewDescription
        {
            get { return Step == ProcessStepEnum.ViewDescription; }
        }

        public bool CanConfigure
        {
            get { return Step == ProcessStepEnum.Configure; }
        }

        public bool CanViewResults
        {
            get { return Step == ProcessStepEnum.Run && Results != null && Results.Count > 0; }
        }

        public void AdvanceStep()
        {
            switch (Step)
            {
                case ProcessStepEnum.ViewDescription:
                    Step = ProcessStepEnum.Configure;
                    break;

                case ProcessStepEnum.Configure:
                    Step = ProcessStepEnum.Run;
                    break;

                case ProcessStepEnum.Run:
                    Step = ProcessStepEnum.ViewResults;
                    break;
            }
        }

        public void NotifyProcessStatusChanged(ProcessStatusChangedCallbackEventArgs e)
        {
            ProcessStatusMessages.Add(e.NewStatus);
        }

        public abstract Task RunProcessAsync(ProcessStatusChangedCallback callback);
    }
}
