<?php
error_reporting(E_ALL);
ini_set('display_errors', 'On');

require_once "MarkClient.php";
$client = new MarkClient();
?>

<h1>Multi Agent Ranking Framework</h1>
<p><?php echo date("Y-m-d H:i:s", time()) ?></p>

<?php
$evidences = $client->findEvidence("detection.readwrite");
?>

<table>
<?php foreach ($evidences as $evidence) : ?>

  <tr>
    <td><?= $evidence->subject ?></td>
    <td><?= $evidence->score ?></td>
    <td><?= date("Y-m-d H:i:s", $evidence->time) ?></td>
  </tr>
<?php endforeach; ?>
</table>

