import React, {ChangeEvent, FC, useCallback, useEffect, useState} from "react"
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
  TableRow,
  Tooltip,
  Typography
} from "@mui/material"
import dayjs from "dayjs"
import AddOverride from "./AddOverride"
import {Add, Cancel, Delete, Person, Save} from "@mui/icons-material"
import {LocalisationOverride} from "./types"
import sortBy from "lodash.sortby"
import {OphButton} from "@opetushallitus/oph-design-system"
import {useTranslate} from "@tolgee/react"

interface Props {
  showMessage: (message: React.ReactNode) => void
}

const Editable = styled(Typography)({
  cursor: "pointer"
})

const LocalisationOverrides: FC<Props> = ({showMessage}) => {
  const [overrides, setOverrides] = useState<LocalisationOverride[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState<boolean>(false)
  const [deleteDialogId, setDeleteDialogId] = useState<number>(0)
  const [editLocalisation, setEditLocalisation] = useState<LocalisationOverride | null>(null)
  const {t} = useTranslate()
  const loadOverrides = useCallback(() => {
    if (showMessage && t) {
      fetch("/lokalisointi/api/v1/override", {
        method: "GET",
        credentials: "same-origin"
      })
        .then(async (res) => {
          const body = await res.json()
          if (!res.ok) {
            showMessage(t("loading-overrides-failed", "Yliajojen lataaminen ei onnistunut: {body}", {
              body: JSON.stringify(body)
            }))
            return
          }
          setOverrides(sortBy(body, ["namespace", "key", "locale"]))
        })
    }
  }, [showMessage, t])
  useEffect(() => {
    loadOverrides()
  }, [loadOverrides, showMessage])
  const deleteOverride = (id: number) => {
    fetch(`/lokalisointi/api/v1/override/${id}`, {
      method: "DELETE",
      credentials: "same-origin"
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          showMessage(t("delete-override-failed", "Yliajon poistaminen ei onnistunut: {body}", {
            body: JSON.stringify(body)
          }))
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
          showMessage(t("save-override-failed", "Yliajon tallentaminen ei onnistunut: {body}", {
            body: JSON.stringify(body)
          }))
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
        <Typography variant="h4">{t("overrides-title", "Käännösten yliajot")}</Typography>
      </Grid>
      <Grid size={12}>
        <Typography variant="body1">{t("overrides-info", "Yliajojen kuvausteksti")}</Typography>
      </Grid>
      <Grid size={12}>
        <TableContainer component={Paper}>
          <Table sx={{minWidth: 650}} aria-label={t("override-listing", "listaus käännösten yliajoista")}>
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
                        sx={{display: 'flex', alignItems: 'center', minWidth: '300px'}}
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
                        <IconButton type="button" sx={{p: '8px'}} aria-label={t("save", "tallenna")} size="small"
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
                    <Tooltip title={override.createdBy}>
                      <Person/>
                    </Tooltip>
                  </TableCell>
                  <TableCell>{dayjs(override.updated).format("L LT")}</TableCell>
                  <TableCell>
                    <Tooltip title={override.updatedBy}>
                      <Person/>
                    </Tooltip>
                  </TableCell>
                  <TableCell>
                    <Tooltip title={t("delete", "poista")}>
                      <IconButton onClick={() => setDeleteDialogId(override.id)}><Delete/></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {!overrides && (
                <TableRow>
                  <TableCell colSpan={10}>{t("no-overrides", "Ei yliajoja")}</TableCell>
                </TableRow>
              )}

              {!addDialogOpen ? (
                <TableRow>
                  <TableCell colSpan={10} align="center">
                    <Tooltip title={t("add-new-override", "lisää uusi")}>
                      <IconButton onClick={() => {
                        setAddDialogOpen(true)
                      }}><Add/></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ) : (
                <AddOverride close={() => {
                  setAddDialogOpen(false)
                  loadOverrides()
                }} showMessage={showMessage}/>
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
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            {t("delete-override-description", "Haluatko varmasti poistaa käännöksen yliajon?")}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <OphButton onClick={deleteDialogClose}>{t("delete-cancel", "Peruuta")}</OphButton>
          <OphButton onClick={() => deleteOverride(deleteDialogId!)} autoFocus>
            {t("delete-action", "Poista")}
          </OphButton>
        </DialogActions>
      </Dialog>
    </Grid>
  )
}

export default LocalisationOverrides