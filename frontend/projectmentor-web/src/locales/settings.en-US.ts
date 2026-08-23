export default {
  navigation: 'Settings',
  title: 'Settings',
  description: 'Manage workspace display preferences for this browser.',
  experience: {
    title: 'Experience',
    current: 'Current Experience',
    description: 'Choose how PMAI is presented. Switching does not sign you out or change project data.',
    classic: {
      title: 'Classic Experience',
      description: 'Original PMAI interface'
    },
    workbench: {
      title: 'Evidence Workbench',
      description: 'Evidence-first workspace'
    },
    selected: 'Current',
    disabled: 'Evidence Workbench is not enabled in this environment.',
    unavailable: 'This experience is not available for this page yet.',
    fallbackDescription: 'Classic Experience is being used automatically. Your route and sign-in state are unchanged.',
    onlyWorkbench: 'This feature is only available in Evidence Workbench.',
    onlyClassic: 'This feature is only available in Classic Experience.',
    unavailableFeature: 'This feature is not available in the selected experience.',
    switchToWorkbench: 'Switch to Evidence Workbench',
    switchToClassic: 'Switch to Classic Experience'
  }
}
