import React, {ChangeEvent, FC, useCallback, useEffect, useState} from "react"
import {
  Button,
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
  const loadOverrides = useCallback(() => {
    fetch("/lokalisointi/api/v1/override", {
      method: "GET",
      credentials: "same-origin"
    })
      .then(async (res) => {
        const body = await res.json()
        if (!res.ok) {
          showMessage(`Yliajojen lataaminen ei onnistunut: ${JSON.stringify(body)}`)
          return
        }
        setOverrides(sortBy(body, ["namespace", "key", "locale"]))
      })
  }, [showMessage])
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
          showMessage(`Yliajon poistaminen ei onnistunut: ${JSON.stringify(body)}`)
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
          showMessage(`Yliajon tallentaminen ei onnistunut: ${JSON.stringify(body)}`)
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
        <Typography variant="h4">Käännösten yliajot</Typography>
      </Grid>
      <Grid size={12}>
        <Typography variant="body1">Yliajojen kuvausteksti TODO</Typography>
      </Grid>
      <Grid size={12}>
        <TableContainer component={Paper}>
          <Table sx={{minWidth: 650}} aria-label="listaus käännösten yliajoista">
            <TableHead>
              <TableRow>
                <TableCell>tunniste</TableCell>
                <TableCell width="10%">nimiavaruus</TableCell>
                <TableCell width="20%">avain</TableCell>
                <TableCell width="7%">kieli</TableCell>
                <TableCell width="30%">arvo</TableCell>
                <TableCell>luontiaika</TableCell>
                <TableCell>luonut</TableCell>
                <TableCell>päivitysaika</TableCell>
                <TableCell>päivittänyt</TableCell>
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
                        <IconButton type="button" sx={{p: '8px'}} aria-label="tallenna" size="small"
                                    disabled={editLocalisation.value === overrides.find(o => o.id === override.id)?.value}
                                    onClick={saveOverride(override.id)} color="primary">
                          <Save/>
                        </IconButton>
                        <IconButton sx={{p: '6px'}} aria-label="peruuta" size="small" onClick={editDialogClose}>
                          <Cancel/>
                        </IconButton>
                      </Paper>
                    ) : (
                      <Editable aria-label="muokattavissa" tabIndex={0}
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
                    <Tooltip title="poista">
                      <IconButton onClick={() => setDeleteDialogId(override.id)}><Delete/></IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {!overrides && (
                <TableRow>
                  <TableCell colSpan={10}>Ei yliajoja</TableCell>
                </TableRow>
              )}

              {!addDialogOpen ? (
                <TableRow>
                  <TableCell colSpan={10} align="center">
                    <Tooltip title="lisää uusi">
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
          Poista yliajo?
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Haluatko varmasti poistaa käännöksen yliajon?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={deleteDialogClose}>Peruuta</Button>
          <Button onClick={() => deleteOverride(deleteDialogId!)} autoFocus>
            Poista
          </Button>
        </DialogActions>
      </Dialog>
    </Grid>
  )
}

export default LocalisationOverrides