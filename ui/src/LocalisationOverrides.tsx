import React, {ChangeEvent, useCallback, useEffect, useState} from "react"
import {
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle, Divider,
  Grid2 as Grid,
  IconButton, InputBase,
  Paper,
  styled,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow
} from "@mui/material"
import dayjs from "dayjs"
import sortBy from "lodash.sortby"
import {Add, Cancel, Close, Delete, Person, Save} from "@mui/icons-material"
import {OphButton, OphTypography} from "@opetushallitus/oph-design-system"
import {useTranslate} from "@tolgee/react"
import {AddOverride} from "./AddOverride"
import {LocalisationOverride} from "./types"
import {useSnackbar} from "notistack"

const Editable = styled(OphTypography)(({theme}) => `
    cursor: pointer;
    border: 1px solid ${theme.palette.divider};
    padding: 9px;
    min-width: 300px;
`)

export const LocalisationOverrides: React.FC = () => {
  const [overrides, setOverrides] = useState<LocalisationOverride[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState<boolean>(false)
  const [deleteDialogId, setDeleteDialogId] = useState<number>(0)
  const [editLocalisation, setEditLocalisation] = useState<LocalisationOverride | null>(null)
  const {t} = useTranslate()
  const {enqueueSnackbar} = useSnackbar()
  const loadOverrides = useCallback(() => {
    if (enqueueSnackbar && t) {
      fetch("/lokalisointi/api/v1/override", {
        method: "GET",
        credentials: "same-origin"
      })
        .then(async (res) => {
          const body = await res.json()
          if (!res.ok) {
            enqueueSnackbar(t("loading-overrides-failed", {
              body: JSON.stringify(body)
            }), {variant: "error"})
            return
          }
          setOverrides(sortBy(body, ["namespace", "key", "locale"]))
        })
    }
  }, [enqueueSnackbar, t])
  useEffect(() => {
    loadOverrides()
  }, [loadOverrides, enqueueSnackbar])
  const deleteOverride = (id: number) => {
    fetch(`/lokalisointi/api/v1/override/${id}`, {
      method: "DELETE",
      credentials: "same-origin"
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          enqueueSnackbar(t("delete-override-failed", {
            body: JSON.stringify(body)
          }), {variant: "error"})
          return
        }
        deleteDialogClose()
        loadOverrides()
      })
  }
  const deleteDialogClose = () => {
    setDeleteDialogId(0)
  }
  const editDialogClose = () => {
    setEditLocalisation(null)
  }
  const saveOverride = (id: number) => () => {
    const override = overrides.find((o) => o.id === id)
    if (override) {
      fetch(`/lokalisointi/api/v1/override/${id}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        credentials: "same-origin",
        body: JSON.stringify({
          namespace: override.namespace,
          locale: override.locale,
          key: override.key,
          value: editLocalisation!.value
        })
      }).then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          enqueueSnackbar(t("save-override-failed", {
            body: JSON.stringify(body)
          }), {variant: "error"})
          return
        }
        editDialogClose()
        return loadOverrides()
      })
    }
  }
  return (
    <Grid container spacing={3}>
      <Grid size={12}>
        <OphTypography variant="h2">{t("overrides-title", "Käännösten yliajot")}</OphTypography>
      </Grid>
      <Grid size={12}>
        <OphTypography variant="body1">{t("overrides-info", "Yliajojen kuvausteksti")}</OphTypography>
      </Grid>
      <Grid size={12}>
        <TableContainer component={Paper} elevation={0}>
          <Table sx={{minWidth: 650}} aria-label={t("override-listing", "listaus käännösten yliajoista")}
                 size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t("override-id", "tunniste")}</TableCell>
                <TableCell width="10%">{t("override-namespace", "nimiavaruus")}</TableCell>
                <TableCell width="20%">{t("override-key", "avain")}</TableCell>
                <TableCell width="7%">{t("override-locale", "kieli")}</TableCell>
                <TableCell width="30%">{t("override-value", "arvo")}</TableCell>
                <TableCell>{t("override-created", "luontiaika")}</TableCell>
                <TableCell>{t("override-created-by", "luonut")}</TableCell>
                <TableCell>{t("override-updated", "päivitysaika")}</TableCell>
                <TableCell>{t("override-updated-by", "päivittänyt")}</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {overrides.map((override, i) => (
                <TableRow key={i}>
                  <TableCell component="th" scope="row">
                    {override.id}
                  </TableCell>
                  <TableCell>{override.namespace}</TableCell>
                  <TableCell>{override.key}</TableCell>
                  <TableCell>{override.locale}</TableCell>
                  <TableCell>
                    {!!editLocalisation && override.id === editLocalisation.id ? (
                      <Paper
                        component="form" variant="outlined"
                        sx={theme => ({
                          display: 'flex',
                          alignItems: 'center',
                          minWidth: '300px',
                          borderRadius: '2px',
                          borderColor: theme.palette.grey["400"],
                          padding: '1px',
                          '&:focus-within,&:hover': {
                            borderColor: theme.palette.primary.main,
                            borderWidth: '2px',
                            padding: 0
                          },
                        })}
                      >
                        <InputBase
                          sx={{ml: 1, flex: 1, pb: 0}}
                          value={editLocalisation.value}
                          size="small"
                          onChange={(e: ChangeEvent<HTMLTextAreaElement>) => {
                            setEditLocalisation({...editLocalisation, value: e.target.value})
                          }}
                          multiline
                          inputProps={{'aria-label': 'arvo'}}
                        />
                        <Divider sx={{height: 20, m: 0.5}} orientation="vertical"/>
                        <IconButton type="button" sx={{p: '8px'}} aria-label={t("save", "tallenna")}
                                    size="small"
                                    disabled={editLocalisation.value === overrides.find(o => o.id === override.id)?.value}
                                    onClick={saveOverride(override.id)} color="primary">
                          <Save/>
                        </IconButton>
                        <IconButton sx={{p: '6px'}} aria-label={t("cancel", "peruuta")} size="small"
                                    onClick={editDialogClose}>
                          <Cancel/>
                        </IconButton>
                      </Paper>
                    ) : (
                      <Editable aria-label={t("editable", "muokattavissa")} tabIndex={0}
                                onClick={() => setEditLocalisation(override)}
                                onKeyDown={(e) => {
                                  if (e.key === 'Enter' || e.key === ' ') {
                                    setEditLocalisation(override)
                                  }
                                }}>
                        {override.value}
                      </Editable>
                    )}
                  </TableCell>
                  <TableCell>{dayjs(override.created).format("L LT")}</TableCell>
                  <TableCell>
                    <span title={override.createdBy}>
                      <Person/>
                    </span>
                  </TableCell>
                  <TableCell>{dayjs(override.updated).format("L LT")}</TableCell>
                  <TableCell>
                    <span title={override.updatedBy}>
                      <Person/>
                    </span>
                  </TableCell>
                  <TableCell>
                    <IconButton onClick={() => setDeleteDialogId(override.id)}
                                title={t("delete", "poista")}><Delete/></IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {(!overrides || overrides.length === 0) && (
                <TableRow>
                  <TableCell colSpan={10} align="center">{t("no-overrides", "Ei yliajoja")}</TableCell>
                </TableRow>
              )}

              {!addDialogOpen ? (
                <TableRow>
                  <TableCell colSpan={10} align="right" sx={{borderBottom: "none"}}>
                    <OphButton variant="outlined" startIcon={<Add/>}
                               onClick={() => setAddDialogOpen(true)}>{t("add-new-override", "Uusi yliajo")}</OphButton>
                  </TableCell>
                </TableRow>
              ) : (
                <AddOverride close={() => {
                  setAddDialogOpen(false)
                  loadOverrides()
                }}/>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Grid>
      <Dialog open={deleteDialogId > 0}
              onClose={deleteDialogClose}
              aria-labelledby="delete-dialog-title"
              aria-describedby="delete-dialog-description">
        <DialogTitle id="delete-dialog-title">
          {t("delete-override-title", "Poista yliajo?")}
        </DialogTitle>
        <IconButton onClick={deleteDialogClose} sx={theme => ({
          position: "absolute",
          top: theme.spacing(1),
          right: theme.spacing(1)
        })}><Close/></IconButton>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            {t("delete-override-description", "Haluatko varmasti poistaa käännöksen yliajon?")}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={theme => ({p: theme.spacing(2)})}>
          <OphButton variant="outlined"
                     onClick={deleteDialogClose}
                     sx={theme => ({mr: theme.spacing(0.5)})}>{t("delete-cancel", "Peruuta")}</OphButton>
          <OphButton variant="contained"
                     color="primary"
                     autoFocus
                     onClick={() => deleteOverride(deleteDialogId!)}>
            {t("delete-action", "Poista")}
          </OphButton>
        </DialogActions>
      </Dialog>
    </Grid>
  )
}
