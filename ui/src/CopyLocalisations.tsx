import React, {useState} from "react"
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid2 as Grid,
  IconButton,
  LinearProgress,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText
} from "@mui/material"
import {Close, ContentCopy} from "@mui/icons-material"
import {OphButton, OphCheckbox, OphSelect, OphTypography} from "@opetushallitus/oph-design-system"
import {useTranslate} from "@tolgee/react"
import {useSnackbar} from "notistack"
import {UIConfig} from "./types"

interface Props {
  uiConfig: UIConfig,
}

interface CopyRequest {
  source: string
  namespaces?: string[]
}

export const CopyLocalisations: React.FC<Props> = ({uiConfig}) => {
  const [source, setSource] = useState<string>("")
  const [availableNamespaces, setAvailableNamespaces] = useState<string[] | undefined>(undefined)
  const [selectedNamespaces, setSelectedNamespaces] = useState<string[]>([])
  const [chooseNamespacesModalOpen, setChooseNamespacesModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const {t} = useTranslate()
  const {enqueueSnackbar} = useSnackbar()
  const loadAvailableNamespaces = (sourceEnvironment: string) => {
    setAvailableNamespaces(undefined)
    setLoading(true)
    fetch(`/lokalisointi/api/v1/copy/available-namespaces?source=${sourceEnvironment}`, {
      method: "GET"
    }).then(async (res) => {
      const body = await res.json()
      if (!res.ok) {
        enqueueSnackbar(t("namespaces-could-not-be-loaded", "Nimiavaruuksia ei saatu ladattua. Yritä myöhemmin uudelleen."), {variant: "error"})
        setLoading(false)
        return
      }
      setAvailableNamespaces(body.sort())
      setLoading(false)
    }).catch(err => console.log(`Error fetching available namespaces: ${err}`))
  }

  const copy = () => {
    const request: CopyRequest = {source: source}
    if (selectedNamespaces.length > 0) {
      request.namespaces = selectedNamespaces
    }
    setLoading(true)
    fetch("/lokalisointi/api/v1/copy", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "same-origin",
      body: JSON.stringify(request)
    }).then(async (res) => {
      const body = await res.json()
      if (!res.ok) {
        enqueueSnackbar(t("copy-translations-failed", {
          body: JSON.stringify(body)
        }), {variant: "error"})
        return
      }
      setSource("")
      setAvailableNamespaces(undefined)
      setSelectedNamespaces([])
      enqueueSnackbar(JSON.stringify(res), {variant: "success"})
      setLoading(false)
    })
  }
  const closeNamespaceChooseDialog = () => {
    setChooseNamespacesModalOpen(false)
  }
  return (
    <>
      <Grid container spacing={3}>
        <Grid size={12}>
          <OphTypography
            variant="h2">{t("copy-translations-title", "Käännösten kopiointi ympäristöstä toiseen")}</OphTypography>
        </Grid>
        <Grid size={12}>
          <OphTypography variant="label">{t("copy-source", "lähdeympäristö")}</OphTypography>
          <OphTypography variant="body2">
            {t("copy-source-help", "käännökset kopioidaan lähdeympäristöstä ympäristöön {target}", {
              target: !!uiConfig.currentEnvironment ? t(`env-${uiConfig.currentEnvironment}`, uiConfig.currentEnvironment) : "-"
            })}
          </OphTypography>
          <OphSelect id="copy-source" value={source} fullWidth size="small"
                     sx={theme => ({mt: theme.spacing(0.5)})}
                     onChange={(e) => {
                       const value = e.target.value
                       setSource(value)
                       loadAvailableNamespaces(value)
                     }}
                     options={uiConfig.sourceEnvironments?.map(
                       (environment) => ({
                         label: t(`env-${environment}`, environment),
                         value: environment
                       })
                     ) ?? []}/>
        </Grid>
        <Grid size={12}>
          <OphTypography variant="label">
            {t("copy-namespaces", "Kopioitavat nimiavaruudet")}
          </OphTypography>
          {selectedNamespaces.length > 0 ? (
            <OphTypography component="div" variant="body2">
              {t("chosen-namespaces", "Valittuna {namespaces}", {
                namespaces: selectedNamespaces.join(", ")
              })}
            </OphTypography>
          ) : <OphTypography component="div" variant="body2">
            {t("default-all-namespaces", "Oletuksena valittuna on kaikki nimiavaruudet")}
          </OphTypography>}
          <OphButton fullWidth variant="outlined" disabled={!availableNamespaces} size="large"
                     onClick={() => setChooseNamespacesModalOpen(true)} sx={theme => ({mt: theme.spacing(0.5)})}>
            {t("choose-namespaces", "Valitse")}
          </OphButton>
        </Grid>
        <Grid size={12}>
          <OphButton fullWidth variant="contained" onClick={copy} color="primary" size="large"
                     disabled={loading || !source || !availableNamespaces}
                     startIcon={<ContentCopy/>}>
            {t("copy", "Kopioi")}
          </OphButton>
        </Grid>
        <Dialog open={chooseNamespacesModalOpen}
                onClose={closeNamespaceChooseDialog}
                maxWidth="sm"
                aria-labelledby="choose-dialog-title"
                aria-describedby="choose-dialog-description">
          <DialogTitle id="choose-dialog-title">
            {t("choose-namespaces", "Valitse kopioitavat nimiavaruudet")}
          </DialogTitle>
          <IconButton onClick={closeNamespaceChooseDialog} sx={theme => ({
            position: "absolute",
            top: theme.spacing(1),
            right: theme.spacing(1)
          })}><Close/></IconButton>
          <DialogContent>
            <DialogContentText id="choose-dialog-description">
              {t("copy-namespaces-help", "Valitse ne nimiavaruudet, jotka haluat kopioida. Jätä valitsematta mitään, jos haluat kopioida kaikki nimiavaruudet.")}
            </DialogContentText>
            <List sx={{width: '100%', bgcolor: 'background.paper'}}>
              {!!availableNamespaces && availableNamespaces.map(
                (ns, i) => {
                  const labelId = `checkbox-list-label-${i}`
                  const handleToggle = (value: string) => () => {
                    if (!selectedNamespaces.includes(value)) {
                      setSelectedNamespaces((namespaces) => [...namespaces, ns].sort())
                    } else {
                      setSelectedNamespaces((namespaces) => namespaces.filter(n => n !== ns))
                    }
                  }
                  return (
                    <ListItem
                      key={i}
                      disablePadding
                    >
                      <ListItemButton role={undefined} onClick={handleToggle(ns)} dense>
                        <ListItemIcon sx={theme => ({minWidth: theme.spacing(3)})}>
                          <OphCheckbox
                            edge="start"
                            checked={selectedNamespaces.includes(ns)}
                            tabIndex={-1}
                            inputProps={{'aria-labelledby': labelId}}
                          />
                        </ListItemIcon>
                        <ListItemText id={labelId} primary={ns}/>
                      </ListItemButton>

                    </ListItem>
                  )
                })}
            </List>
          </DialogContent>
          <DialogActions>
            <OphButton variant="outlined"
                       onClick={closeNamespaceChooseDialog}
                       sx={theme => ({mr: theme.spacing(0.5)})}>{t("cancel", "Peruuta")}</OphButton>
            <OphButton variant="contained"
                       color="primary"
                       onClick={() => {
                         setChooseNamespacesModalOpen(false)
                       }}>{t("choose", "Valitse")}</OphButton>
          </DialogActions>
        </Dialog>
      </Grid>
      {loading && <LinearProgress sx={theme => ({
        position: "absolute",
        bottom: 0,
        left: 0,
        width: "100%",
        height: theme.spacing(1),
        borderBottomLeftRadius: theme.spacing(0.5),
        borderBottomRightRadius: theme.spacing(0.5)
      })}/>}
    </>
  )
}
